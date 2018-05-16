package com.icthh.xm.commons.config.client.service;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class CommonConfigService implements ConfigService {

    private final CommonConfigRepository commonConfigRepository;

    private Consumer<Configuration> configurationListener;

    @Override
    public Map<String, Configuration> getConfigurationMap(String commit) {
        return commonConfigRepository.getConfig(commit);
    }

    @Override
    public void onConfigurationChanged(Consumer<Configuration> configurationListener) {
        this.configurationListener = configurationListener;
    }

    /**
     * Update configuration from config service
     *
     * @param commit commit hash, will be empty if configuration deleted
     * @param paths collection of paths updated
     */
    @Override
    public void updateConfigurations(String commit, Collection<String> paths) {
        Map<String, Configuration> configurationsMap = commonConfigRepository.getConfig(commit);
        paths.forEach(path -> notifyUpdated(configurationsMap
            .getOrDefault(path, new Configuration(path, null))));
    }

    private void notifyUpdated(Configuration configuration) {
        log.debug("Notify configuration changed [{}]", configuration.getPath());
        Optional.ofNullable(configurationListener)
            .ifPresent(configurationListener -> configurationListener.accept(configuration));
    }
}
