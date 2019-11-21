package com.icthh.xm.commons.config.client.api;

import com.icthh.xm.commons.config.domain.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public abstract class AbstractConfigService implements ConfigService {

    private final List<ConfigurationChangedListener> configurationListeners = new ArrayList<>();

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
        Map<String, Configuration> configurationsMap = getConfigurationMap(commit, paths);
        paths.forEach(path -> notifyUpdated(getNonNullConfiguration(configurationsMap, path)));
    }

    protected void notifyUpdated(Configuration configuration) {
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
}
