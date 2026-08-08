package com.icthh.xm.commons.web.spring.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.WebResourceRoot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Switches the Tomcat web resources archive index strategy from the default {@code SIMPLE} to
 * {@code BLOOM} and builds the index during startup.
 *
 * <p>Why. In a packed executable WAR ({@code java -jar app.war}) every classloader lookup that misses falls
 * through to Tomcat's {@code JarWarResourceSet}, which streams whole nested jars to answer "not found" -
 * and with the {@code SIMPLE} strategy the streamed entry index is dropped by the container background gc
 * every few seconds, so the cost repeats. Groovy metaclass initialization of the lep runtime issues
 * thousands of such misses ({@code <Class>BeanInfo}, {@code <Class>Customizer},
 * {@code groovy.runtime.metaclass.*}) through the webapp classloader, which Tomcat sets as the context
 * classloader of request threads: measured on a real service this made the first lep execution after a
 * restart take minutes. The {@code BLOOM} strategy builds a bloom filter over the entries of every nested
 * jar once and retains it across the background gc, so every later miss is answered in constant time.
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

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatBloomArchiveIndexCustomizer() {
        return factory -> factory.addContextCustomizers(context ->
            context.addLifecycleListener(new BloomArchiveIndexInstaller()));
    }

    static class BloomArchiveIndexInstaller implements LifecycleListener {

        /**
         * A name that never resolves. One lookup through the webapp classloader forces Tomcat to index
         * every nested jar of a packed WAR (building the retained bloom filters) before it can answer
         * "not found" - the exact cost the first classloader miss of a request thread would otherwise pay.
         * The webapp classloader is reachable only from the Tomcat context, which is why the probe lives
         * here and not in the lep warmup: the lep code runs with the application (launcher) classloader,
         * whose misses are cheap.
         */
        static final String MISS_PROBE_CLASS_NAME =
            "groovy.runtime.metaclass.TomcatArchiveIndexWarmupMissProbeMetaClass";

        @Override
        public void lifecycleEvent(LifecycleEvent event) {
            if (!(event.getLifecycle() instanceof Context context)) {
                return;
            }
            switch (event.getType()) {
                // CONFIGURE_START fires after the context created and started its resource root
                case Lifecycle.CONFIGURE_START_EVENT -> enableBloomArchiveIndex(context);
                // by AFTER_START the webapp classloader exists: build the index off the request path
                case Lifecycle.AFTER_START_EVENT -> buildArchiveIndexAsync(context);
                default -> { }
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
            ClassLoader webappClassLoader = context.getLoader() != null ? context.getLoader().getClassLoader() : null;
            if (webappClassLoader == null) {
                log.warn("Webapp classloader not available, archive index will be built by the first miss");
                return;
            }
            Thread thread = new Thread(() -> buildArchiveIndex(webappClassLoader), "tomcat-archive-index-warmup");
            thread.setDaemon(true);
            thread.start();
        }

        private void buildArchiveIndex(ClassLoader webappClassLoader) {
            long startNanos = System.nanoTime();
            try {
                Class.forName(MISS_PROBE_CLASS_NAME, false, webappClassLoader);
                log.warn("Archive index warmup probe unexpectedly resolved {}", MISS_PROBE_CLASS_NAME);
            } catch (ClassNotFoundException expected) {
                log.info("Tomcat archive index built in {} ms", (System.nanoTime() - startNanos) / 1_000_000L);
            } catch (Throwable e) {
                log.warn("Tomcat archive index warmup failed: {}", e.toString());
            }
        }
    }
}
