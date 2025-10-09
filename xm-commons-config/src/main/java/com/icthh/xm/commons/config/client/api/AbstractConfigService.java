package com.icthh.xm.commons.config.client.api;

import com.icthh.xm.commons.config.domain.Configuration;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.AntPathMatcher;

@Slf4j
public abstract class AbstractConfigService implements ConfigService {

    private final List<ConfigurationChangedListener> configurationListeners = new ArrayList<>();
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Value("${application.config-fetch-all.enabled:false}")
    private boolean isFetchAll;

    @Value("${spring.application.name}")
    private String applicationName;

    public static List<String> CONFIG_ANT_PATTERN_PATHS;

    @PostConstruct
    public void init() {
        CONFIG_ANT_PATTERN_PATHS = List.of(
                "/config/tenants/commons/**",
                "/config/tenants/*",
                "/config/tenants/{tenantName}/commons/**",
                "/config/tenants/{tenantName}/*",
                "/config/tenants/{tenantName}/" + applicationName + "/**",
                "/config/tenants/{tenantName}/config/**");
    }

    @Override
    public void addConfigurationChangedListener(ConfigurationChangedListener configurationListener) {
        this.configurationListeners.add(configurationListener);
    }

    /**
     * Update configuration from config service
     *
     * @param commit commit hash, will be empty if configuration deleted
     * @param paths collection of paths updated
     */
    @Override
    public void updateConfigurations(String commit, Collection<String> paths) {
        final Collection<String> filteredPaths = getFilteredPaths(paths);
        if (!filteredPaths.isEmpty()) {
            Map<String, Configuration> configurationsMap = getConfigurationMap(commit, filteredPaths);
            paths.forEach(path -> notifyUpdated(getNonNullConfiguration(configurationsMap, path)));
            configurationListeners.forEach(it -> it.refreshFinished(filteredPaths));
        }
    }

    public void notifyUpdated(Configuration configuration) {
        log.debug("Notify configuration changed [{}]", configuration != null ? configuration.getPath() : null);

        if (configuration == null) {
            return;
        }
        configurationListeners.forEach(configurationListener ->
            configurationListener.onConfigurationChanged(configuration));
    }

    private Configuration getNonNullConfiguration(final Map<String, Configuration> configurationsMap,
                                                  final String path) {
        return Optional.ofNullable(configurationsMap.get(path))
                       .orElseGet(() -> new Configuration(path, null));
    }

    private Collection<String> getFilteredPaths(Collection<String> paths) {
        if (isFetchAll) {
            return paths;
        }

        return paths.stream()
                .filter(path -> matchPath(path, CONFIG_ANT_PATTERN_PATHS))
                .collect(Collectors.toList());
    }

    private boolean matchPath(String path, List<String> pathsAntPattern) {
        return pathsAntPattern.stream().anyMatch(it -> antPathMatcher.match(it, path));
    }
}
