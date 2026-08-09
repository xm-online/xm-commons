package com.icthh.xm.commons.web.spring.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.WebResourceRoot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Makes classloader lookups cheap in a packed executable WAR ({@code java -jar app.war}).
 *
 * <p>In a packed WAR every classloader lookup that Tomcat cannot answer from an index streams whole nested
 * jars through {@code JarWarResourceSet} (jar-in-war has no random access). The lep runtime issues
 * thousands of such lookups on the first script execution ({@code <Class>BeanInfo},
 * {@code groovy.runtime.metaclass.*} probes of groovy metaclass initialization) - measured as a first lep
 * call taking 15+ seconds after a service restart. Two measures remove that cost from the request path:
 *
 * <ol>
 * <li>the {@code BLOOM} archive index strategy: every indexed jar keeps a bloom filter that answers
 * definite misses in constant time and survives the container's background cleanup;</li>
 * <li>the whole index is built during startup by calling {@code getArchiveEntries} of every archive
 * resource set. This needs reflection: the method is protected, and no public lookup reaches every set -
 * {@code StandardRoot#getResourceInternal} filters sets by lookup flavor and mount path, and probe lookups
 * through the public API were measured to leave enough sets unindexed for a first request to stream jars
 * for over ten seconds.</li>
 * </ol>
 *
 * <p>Kill switch: {@code application.tomcat-bloom-archive-index=false}.
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = {
    "org.apache.catalina.WebResourceRoot",
    "org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory"
})
@ConditionalOnProperty(name = "application.tomcat-bloom-archive-index", havingValue = "true", matchIfMissing = true)
public class TomcatBloomArchiveIndexConfiguration {

    /**
     * How often the Tomcat context runs its background processing (seconds). On the default cycle the
     * container drops the built archive entry maps via {@code WebResourceRoot.gc()} every ~10s of idle,
     * and the next lookup that needs a dropped map streams the whole nested jar again - measured: an
     * identical build showed a 2.8s first call with this delay applied and a 13.5s first call without it.
     * One day effectively keeps the index for the pod lifetime. Side effect: expired http sessions are
     * also cleaned on this cycle - XM services are stateless (JWT). Set {@code <= 0} to keep the Tomcat
     * default.
     */
    @Value("${application.tomcat-background-processor-delay-seconds:86400}")
    private int backgroundProcessorDelaySeconds;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatBloomArchiveIndexCustomizer() {
        return factory -> factory.addContextCustomizers(context -> {
            if (backgroundProcessorDelaySeconds > 0) {
                context.setBackgroundProcessorDelay(backgroundProcessorDelaySeconds);
                log.info("Tomcat context background processor delay set to {} s", backgroundProcessorDelaySeconds);
            }
            context.addLifecycleListener(new BloomArchiveIndexInstaller());
        });
    }

    /**
     * Switches the resource root to the BLOOM strategy when the context configures and builds the full
     * archive index on a background thread right after the context starts.
     */
    static class BloomArchiveIndexInstaller implements LifecycleListener {

        private static final String ALL_RESOURCES_FIELD = "allResources";
        private static final String GET_ARCHIVE_ENTRIES_METHOD = "getArchiveEntries";

        @Override
        public void lifecycleEvent(LifecycleEvent event) {
            if (!(event.getLifecycle() instanceof Context context)) {
                return;
            }
            switch (event.getType()) {
                // CONFIGURE_START fires after the context created and started its resource root
                case Lifecycle.CONFIGURE_START_EVENT -> enableBloomArchiveIndex(context);
                // by AFTER_START the resources serve lookups: build the index off the request path
                case Lifecycle.AFTER_START_EVENT -> buildArchiveIndexAsync(context);
                default -> log.trace("Ignoring tomcat lifecycle event {}", event.getType());
            }
        }

        private void enableBloomArchiveIndex(Context context) {
            WebResourceRoot resources = context.getResources();
            if (resources == null) {
                log.warn("Tomcat web resources not available, archive index strategy left default");
                return;
            }
            resources.setArchiveIndexStrategy(WebResourceRoot.ArchiveIndexStrategy.BLOOM.name());
            log.info("Tomcat web resources archive index strategy set to BLOOM");
        }

        private void buildArchiveIndexAsync(Context context) {
            WebResourceRoot resources = context.getResources();
            if (resources == null) {
                log.warn("Tomcat web resources not available, archive index will be built by the first miss");
                return;
            }
            Thread thread = new Thread(() -> buildArchiveIndex(resources), "tomcat-archive-index-warmup");
            thread.setDaemon(true);
            thread.start();
        }

        private void buildArchiveIndex(WebResourceRoot resources) {
            long startNanos = System.nanoTime();
            int built = 0;
            try {
                for (Object resourceSet : listResourceSets(resources)) {
                    if (buildArchiveEntries(resourceSet)) {
                        built++;
                    }
                }
                log.info("Tomcat archive index built for {} archive sets in {} ms",
                    built, (System.nanoTime() - startNanos) / 1_000_000L);
            } catch (Throwable e) {
                log.warn("Tomcat archive index warmup failed: {}", e.toString());
            }
        }

        @SuppressWarnings("unchecked")
        private List<Object> listResourceSets(WebResourceRoot resources) throws IllegalAccessException {
            Field allResources = findField(resources.getClass(), ALL_RESOURCES_FIELD);
            if (allResources == null) {
                log.warn("Field {} not found on {}, archive index left to lazy build", ALL_RESOURCES_FIELD,
                    resources.getClass().getName());
                return List.of();
            }
            List<Object> sets = new ArrayList<>();
            for (Object list : (List<?>) allResources.get(resources)) {
                sets.addAll((List<Object>) list);
            }
            return sets;
        }

        private boolean buildArchiveEntries(Object resourceSet) {
            Method getArchiveEntries = findMethod(resourceSet.getClass(), GET_ARCHIVE_ENTRIES_METHOD, boolean.class);
            if (getArchiveEntries == null) {
                return false;
            }
            try {
                getArchiveEntries.setAccessible(true);
                getArchiveEntries.invoke(resourceSet, false);
                return true;
            } catch (Throwable e) {
                log.warn("Error building archive entries of {}: {}", resourceSet, e.toString());
                return false;
            }
        }

        private static Field findField(Class<?> type, String name) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException e) {
                    log.trace("No field {} on {}, searching parent: {}", name, current.getName(), e.toString());
                }
            }
            return null;
        }

        private static Method findMethod(Class<?> type, String name, Class<?>... params) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                try {
                    return current.getDeclaredMethod(name, params);
                } catch (NoSuchMethodException e) {
                    log.trace("No method {} on {}, searching parent: {}", name, current.getName(), e.toString());
                }
            }
            return null;
        }
    }
}
