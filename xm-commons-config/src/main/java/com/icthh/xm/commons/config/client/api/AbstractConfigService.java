package com.icthh.xm.commons.config.client.api;

import com.icthh.xm.commons.config.client.service.ConfigurationOrderService;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.util.AntPathMatcher;

@Slf4j
public abstract class AbstractConfigService implements ConfigService {

    private final List<ConfigurationChangedListener> configurationListeners = new ArrayList<>();
    private final AntPathMatcher antPathMatcher;
    private final FetchConfigurationSettings fetchConfigurationSettings;
    private final ConfigurationOrderService configurationOrderService;

    protected AbstractConfigService(FetchConfigurationSettings fetchConfigurationSettings,
                                    ConfigurationOrderService configurationOrderService) {
        this.antPathMatcher = new AntPathMatcher();
        this.fetchConfigurationSettings = fetchConfigurationSettings;
        this.configurationOrderService = configurationOrderService;
    }

    @Override
    public void addConfigurationChangedListener(ConfigurationChangedListener configurationListener) {
        this.configurationListeners.add(configurationListener);
    }

    /**
     * Update configuration from config service.
     * Paths are dispatched in the order defined by the tenant's order.yml (if any):
     * order.yml itself first, then files by first matching pattern, then the rest.
     *
     * @param commit commit hash, will be empty if configuration deleted
     * @param paths collection of paths updated
     */
    @Override
    public void updateConfigurations(String commit, Collection<String> paths) {
        final Collection<String> filteredPaths = getFilteredPaths(paths);
        if (!filteredPaths.isEmpty()) {
            Map<String, Configuration> configurationsMap = getConfigurationMap(commit, filteredPaths);
            configurationOrderService.processOrderConfigs(filteredPaths, configurationsMap);
            List<String> sortedPaths = configurationOrderService.sortPaths(paths);
            Set<String> filteredSet = new HashSet<>(filteredPaths);
            List<String> sortedFilteredPaths = sortedPaths.stream().filter(filteredSet::contains).toList();
            sortedPaths.forEach(path -> notifyUpdated(getNonNullConfiguration(configurationsMap, path)));
            configurationListeners.forEach(it -> it.refreshFinished(sortedFilteredPaths));
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
        if (fetchConfigurationSettings.getIsFetchAll()) {
            return paths;
        }

        return paths.stream()
                .filter(path -> matchPath(path, fetchConfigurationSettings.getMsConfigPatterns()))
                .toList();
    }

    private boolean matchPath(String path, List<String> pathsAntPattern) {
        return pathsAntPattern.stream().anyMatch(it -> antPathMatcher.match(it, path));
    }
}
