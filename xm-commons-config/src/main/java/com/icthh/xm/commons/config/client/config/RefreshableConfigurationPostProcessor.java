package com.icthh.xm.commons.config.client.config;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.repository.ConfigurationListener;
import com.icthh.xm.commons.config.client.repository.ConfigurationModel;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class RefreshableConfigurationPostProcessor implements BeanPostProcessor, ConfigurationListener {

    public static final String LOG_CONFIG_EMPTY = "<CONFIG_EMPTY>";

    private final ConfigService configService;
    private final ConfigurationModel configurationModel;

    private final Map<String, RefreshableConfiguration> refreshableConfigurations = new HashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof RefreshableConfiguration) {
            refreshableConfigurations.put(beanName, (RefreshableConfiguration) bean);
            log.info("refreshable configuration bean added: {} = {}", beanName, bean.getClass());
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (refreshableConfigurations.containsKey(beanName)) {
            initBean(refreshableConfigurations.get(beanName));
        }
        return bean;
    }

    private void initBean(RefreshableConfiguration refreshableConfiguration) {
        Map<String, Configuration> configMap = configService.getConfig();

        configMap.forEach((key, value) -> {
            if (refreshableConfiguration.isListeningConfiguration(key)) {

                log.info(
                    "Process config init event: [key = {}, size = {}, newHash = {}] in bean: [{}]",
                    key,
                    StringUtils.length(value.getContent()),
                    getValueHash(value.getContent()),
                    getBeanName(refreshableConfiguration));

                refreshableConfiguration.onInit(key, value.getContent());
            }
        });

        log.info("refreshable configuration bean [{}] initialized by configMap with {} entries",
            getBeanName(refreshableConfiguration), configMap.size());
        configurationModel.setConfigurationListener(this);
    }

    @Override
    public void onConfigurationChanged(String path) {
        Map<String, Configuration> configMap = configService.getConfig();
        Configuration configuration = configMap.getOrDefault(path, new Configuration(path, null, null));
        refreshableConfigurations.values().forEach(entry -> onEntryChange(entry, configuration));
    }

    private void onEntryChange(RefreshableConfiguration refreshableConfiguration, Configuration configuration) {
        String configContent = configuration.getContent();

        if (refreshableConfiguration.isListeningConfiguration(configuration.getPath())) {
            refreshableConfiguration.onRefresh(configuration.getPath(), configContent);

            log.info(
                "Process config update event: "
                    + "[path = {}, size = {}, commit = {}, hash = {}] in bean: [{}]",
                configuration.getPath(),
                StringUtils.length(configContent),
                configuration.getCommit(),
                getValueHash(configContent),
                getBeanName(refreshableConfiguration));

        } else {
            log.debug("Ignored config update event: [path = {}, configSize = {} in bean [{}]",
                configuration.getPath(),
                StringUtils.length(configContent),
                getBeanName(refreshableConfiguration));
        }
    }

    private String getBeanName(final RefreshableConfiguration refreshableConfiguration) {
        return refreshableConfiguration.getClass().getSimpleName();
    }

    private String getValueHash(final String configContent) {
        return StringUtils.isEmpty(configContent) ? LOG_CONFIG_EMPTY :
            DigestUtils.md5Hex(configContent);
    }
}
