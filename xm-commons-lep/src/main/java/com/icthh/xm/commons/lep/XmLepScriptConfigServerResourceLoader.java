package com.icthh.xm.commons.lep;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ClassUtils;

import java.time.Instant;
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
    private static final String commonsLepScriptsAntPathPattern = "/config/tenants/{tenantKey}/commons/lep/**";

    // /config/tenant/commons/lep/**
    private static final String environmentLepScriptsAntPathPattern = "/config/tenants/commons/lep/**";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final List<CacheableLepEngine> cacheableEngines;
    private final Boolean fullRecompileOnLepUpdate;

    private ConcurrentHashMap<String, XmLepScriptResource> scriptResources = new ConcurrentHashMap<>();

    public XmLepScriptConfigServerResourceLoader(String appName, List<CacheableLepEngine> cacheableEngines, Boolean fullRecompileOnLepUpdate) {
        Objects.requireNonNull(appName, "appName can't be null");
        this.tenantLepScriptsAntPathPattern = "/config/tenants/{tenantKey}/" + appName + "/lep/**";
        this.cacheableEngines = cacheableEngines;
        this.fullRecompileOnLepUpdate = Boolean.TRUE.equals(fullRecompileOnLepUpdate);
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

        LOGGER.info("LEP xm-ms-config file inited by config path: {}", configKey);
        scriptResources.put(configKey, resource);
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
            scriptResources.compute(updatedKey, (key, currentValue) -> {
                if (currentValue != null) {
                    LOGGER.info("LEP script updated by config path: {}", updatedKey);
                    // update
                    currentValue.update(configContent, getCurrentMilli());
                    return currentValue;
                } else {
                    LOGGER.info("LEP script created by config path: {}", updatedKey);
                    // create
                    return new XmLepScriptResource(updatedKey, configContent, getCurrentMilli());
                }
            });
        }

        if (fullRecompileOnLepUpdate) {
            this.cacheableEngines.forEach(CacheableLepEngine::clearCache);
            scriptResources.forEach((key, value) -> {
                value.modifiedNow();
            });
        } else {
            log.warn("Full recompile on lep update disabled, some class can be cached.");
        }
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
