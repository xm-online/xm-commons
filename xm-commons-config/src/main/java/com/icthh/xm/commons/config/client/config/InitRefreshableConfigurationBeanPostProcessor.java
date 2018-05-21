package com.icthh.xm.commons.config.client.config;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class InitRefreshableConfigurationBeanPostProcessor implements BeanPostProcessor {

    public static final String LOG_CONFIG_EMPTY = "<CONFIG_EMPTY>";

    private final ConfigService configService;

    private final Map<String, RefreshableConfiguration> refreshableConfigurations = new HashMap<>();
    private Map<String, Configuration> configMap;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof RefreshableConfiguration) {
            refreshableConfigurations.put(beanName, (RefreshableConfiguration) bean);
            log.info("refreshable configuration bean added: {} = {}", beanName, bean.getClass());
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (refreshableConfigurations.containsKey(beanName)) {
            initBean(refreshableConfigurations.get(beanName), getConfig());
        }
        return bean;
    }

    private Map<String, Configuration> getConfig() {
        if (configMap == null) {
            configMap = configService.getConfigurationMap(null);
        }
        return configMap;
    }

    private void initBean(RefreshableConfiguration refreshableConfiguration, Map<String, Configuration> configMap) {
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

        configService.addConfigurationChangedListener(configuration -> onEntryChange(refreshableConfiguration, configuration));
    }

    private void onEntryChange(RefreshableConfiguration refreshableConfiguration, Configuration configuration) {
        String configContent = configuration.getContent();

        if (refreshableConfiguration.isListeningConfiguration(configuration.getPath())) {
            refreshableConfiguration.onRefresh(configuration.getPath(), configContent);

            log.info(
                "Process config update event: "
                    + "[path = {}, size = {}, hash = {}] in bean: [{}]",
                configuration.getPath(),
                StringUtils.length(configContent),
                getValueHash(configContent),
                getBeanName(refreshableConfiguration));
        } else {
            log.debug("Ignored config update event: [path = {}, configSize = {} in bean [{}]",
                configuration.getPath(),
                StringUtils.length(configContent),
                getBeanName(refreshableConfiguration));
        }
    }

    private static String getBeanName(final RefreshableConfiguration refreshableConfiguration) {
        return refreshableConfiguration.getClass().getSimpleName();
    }

    private static String getValueHash(final String configContent) {
        return StringUtils.isEmpty(configContent) ? LOG_CONFIG_EMPTY :
            DigestUtils.md5Hex(configContent);
    }
}
