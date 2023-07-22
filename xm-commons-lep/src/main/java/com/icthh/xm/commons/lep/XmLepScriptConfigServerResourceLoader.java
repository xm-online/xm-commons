package com.icthh.xm.commons.lep;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@link XmLepScriptConfigServerResourceLoader} class.
 */
@Slf4j
public class XmLepScriptConfigServerResourceLoader implements RefreshableConfiguration {

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

    private final Map<String, String> scriptResources = new ConcurrentHashMap<>();


    public XmLepScriptConfigServerResourceLoader(String appName) {
        Objects.requireNonNull(appName, "appName can't be null");
        this.tenantLepScriptsAntPathPattern = "/config/tenants/{tenantKey}/" + appName + "/lep/**";
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
        LOGGER.info("LEP xm-ms-config file inited by config path: {}", configKey);
        scriptResources.put(configKey, configContent);
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
            scriptResources.put(updatedKey, configContent);
        }
    }

    @Override
    public void refreshFinished(Collection<String> paths) {

    }

}
