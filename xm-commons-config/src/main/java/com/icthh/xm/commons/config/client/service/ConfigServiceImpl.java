package com.icthh.xm.commons.config.client.service;

import static com.icthh.xm.commons.config.client.utils.LockUtils.runWithLock;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.client.repository.ConfigRepository;
import com.icthh.xm.commons.config.client.repository.ConfigurationListener;
import com.icthh.xm.commons.config.client.repository.ConfigurationModel;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.locks.Lock;

@Slf4j
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService, ConfigurationModel {

    private final XmConfigProperties xmConfigProperties;
    private final ConfigRepository configRepository;
    private final Lock lock;

    private ConfigurationListener configurationListener;

    @Override
    public Map<String, Configuration> getConfig() {
        return configRepository.getConfig();
    }

    @Override
    public void setConfigurationListener(ConfigurationListener configurationListener) {
        this.configurationListener = configurationListener;
    }

    @Override
    public void updateConfiguration(Configuration configuration) {
        runWithLock(lock, xmConfigProperties.getMaxWaitTimeSecond(), () -> {
            log.debug("Try to update configuration {} {}", configuration.getPath(), configuration.getCommit());
            Configuration old = configRepository.getConfig()
                .getOrDefault(configuration.getPath(), new Configuration(configuration.getPath(), null, null));
            log.debug("Existing configuration {} {}", old.getPath(), old.getCommit());
            if (!configuration.getCommit().equals(old.getCommit())) {
                configRepository.refreshConfig();
            }
            notifyUpdated(configuration.getPath());
        });
    }

    private void notifyUpdated(String path) {
        if (configurationListener != null) {
            log.debug("Notify configuration changed [{}]", path);
            configurationListener.onConfigurationChanged(path);
        }
    }
}
