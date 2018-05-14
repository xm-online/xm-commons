package com.icthh.xm.commons.config.client.service;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.repository.ConfigRepository;
import com.icthh.xm.commons.config.client.repository.ConfigurationModel;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService, ConfigurationModel {

    private final ConfigRepository configRepository;

    private Consumer<Configuration> configurationListener;

    @Override
    public Map<String, Configuration> getConfig() {
        return configRepository.getConfig();
    }

    @Override
    public void onConfigurationChanged(Consumer<Configuration> configurationListener) {
        this.configurationListener = configurationListener;
    }

    @Override
    public void updateConfiguration(Collection<Configuration> configurations) {
        Map<String, Configuration> configurationsMap = configRepository.getConfig();
        configurations.forEach(configuration -> notifyUpdated(configurationsMap
            .getOrDefault(configuration.getPath(), new Configuration(configuration.getPath(), null, null))));
    }

    private void notifyUpdated(Configuration configuration) {
        log.debug("Notify configuration changed [{}]", configuration.getPath());
        Optional.ofNullable(configurationListener)
            .ifPresent(configurationListener -> configurationListener.accept(configuration));
    }
}
