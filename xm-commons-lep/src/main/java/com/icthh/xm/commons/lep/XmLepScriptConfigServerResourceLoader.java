package com.icthh.xm.commons.lep;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ClassUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@link XmLepScriptConfigServerResourceLoader} class.
 */
@Slf4j
public class XmLepScriptConfigServerResourceLoader implements RefreshableConfiguration, ResourceLoader {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(XmLepScriptConfigServerResourceLoader.class);

    /**
     * Pseudo URL prefix for loading from the xm ms config path: "xm-ms-config:"
     */
    public static final String XM_MS_CONFIG_URL_PREFIX = "xm-ms-config:";

    // /config/tenant/{tenant-key}/{ms-name}/lep/**
    private final String tenantLepScriptsAntPathPattern;

    // /config/tenant/{tenant-key}/commons/lep/**
    public static final String commonsLepScriptsAntPathPattern = "/config/tenants/{tenantKey}/commons/lep/**";

    // /config/tenant/commons/lep/**
    public static final String environmentLepScriptsAntPathPattern = "/config/tenants/commons/lep/**";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final List<CacheableLepEngine> cacheableEngines;

    private ConcurrentHashMap<String, XmLepScriptResource> scriptResources = new ConcurrentHashMap<>();

    private Queue<XmLepScriptResource> queueToUpdate = new LinkedBlockingQueue<>();

    public XmLepScriptConfigServerResourceLoader(String appName, List<CacheableLepEngine> cacheableEngines) {
        Objects.requireNonNull(appName, "appName can't be null");
        this.tenantLepScriptsAntPathPattern = "/config/tenants/{tenantKey}/" + appName + "/lep/**";
        this.cacheableEngines = cacheableEngines;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return pathMatcher.match(tenantLepScriptsAntPathPattern, updatedKey)
            || pathMatcher.match(commonsLepScriptsAntPathPattern, updatedKey)
            || pathMatcher.match(environmentLepScriptsAntPathPattern, updatedKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInit(String configKey, String configContent) {
        final String scriptContent = (configContent == null) ? "" : configContent;
        XmLepScriptResource resource = new XmLepScriptResource(configKey, scriptContent, getCurrentMilli());

        LOGGER.info("LEP added to update queue by config path: {}", configKey);
        queueToUpdate.add(resource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRefresh(String updatedKey, String configContent) {
        boolean isDeleted = (configContent == null);
        if (isDeleted) {
            LOGGER.info("LEP script deleted by config path: {}", updatedKey);
            // delete
            scriptResources.remove(updatedKey);
        } else {
            LOGGER.info("LEP added to update queue by config path: {}", updatedKey);
            XmLepScriptResource resource = new XmLepScriptResource(updatedKey, configContent, getCurrentMilli());
            queueToUpdate.add(resource);
        }
    }

    @Override
    public void refreshFinished(Collection<String> paths) {
        Map<String, XmLepScriptResource> updatePart = new HashMap<>(scriptResources);
        while (!queueToUpdate.isEmpty()) {
            XmLepScriptResource scriptResource = queueToUpdate.poll();
            updatePart.put(scriptResource.getDescription(), scriptResource);
        }

        this.cacheableEngines.forEach(it -> it.clearCache(updatePart));
        LOGGER.info("LEP xm-ms-config file inited by configs {}", updatePart.keySet());
        // put source AFTER replace engine, because groovy will not recompile classes with lover modification time then current
        scriptResources.putAll(updatePart);
    }

    private static long getCurrentMilli() {
        return Instant.now().toEpochMilli();
    }

    /**
     * Get LEP script resource.
     *
     * @param location {@code /config/tenant/{tenant-key}/{ms-name}/lep/a/b/c/SomeScript.groovy}
     * @return the LEP script resource
     */
    @Override
    public Resource getResource(String location) {
        String cfgPath = StringUtils.removeStart(location, XM_MS_CONFIG_URL_PREFIX);
        return scriptResources.getOrDefault(cfgPath, XmLepScriptResource.nonExist());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassLoader getClassLoader() {
        return ClassUtils.getDefaultClassLoader();
    }

}
