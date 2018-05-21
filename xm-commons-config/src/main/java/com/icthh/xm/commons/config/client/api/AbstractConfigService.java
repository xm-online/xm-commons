package com.icthh.xm.commons.config.client.api;

import com.icthh.xm.commons.config.domain.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
        Map<String, Configuration> configurationsMap = getConfigurationMap(commit);
        paths.forEach(path -> notifyUpdated(configurationsMap
            .getOrDefault(path, new Configuration(path, null))));
    }

    protected void notifyUpdated(Configuration configuration) {
        log.debug("Notify configuration changed [{}]", configuration.getPath());
        configurationListeners.forEach(configurationListener -> configurationListener.onConfigurationChanged(configuration));
    }
}
