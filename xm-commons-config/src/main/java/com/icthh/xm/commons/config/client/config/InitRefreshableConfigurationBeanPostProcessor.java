package com.icthh.xm.commons.config.client.config;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.api.ConfigService;
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
@Component
@ConditionalOnProperty("xm-config.enabled")
public class InitRefreshableConfigurationBeanPostProcessor implements BeanPostProcessor {

    public static final String LOG_CONFIG_EMPTY = "<CONFIG_EMPTY>";

    private final ConfigService configService;

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
        Map<String, String> configMap = configService.getConfig();

        configMap.forEach((key, value) -> {
            if (refreshableConfiguration.isListeningConfiguration(key)) {

                log.info(
                    "Process config init event: [key = {}, size = {}, newHash = {}] in bean: [{}]",
                    key,
                    StringUtils.length(value),
                    getValueHash(value),
                    getBeanName(refreshableConfiguration));

                refreshableConfiguration.onInit(key, value);
            }
        });

        log.info("refreshable configuration bean [{}] initialized by configMap with {} entries",
                 getBeanName(refreshableConfiguration), configMap.size());
    }

    //TODO:hazel this is called by event listener
    private void onEntryChange(RefreshableConfiguration refreshableConfiguration,
                               Map.Entry<String, String> entry,
                               Map<String, String> configMap) {

        String entryKey = entry.getKey();
        String configContent = configMap.get(entryKey);

        if (refreshableConfiguration.isListeningConfiguration(entryKey)) {

            refreshableConfiguration.onRefresh(entryKey, configContent);

            log.info(
                "Process config update event: "
                + "[key = {}, evtType = {}, size = {}, newHash = {}, oldHash = {}] in bean: [{}]",
                entryKey,
                //entry.getEventType(),
                StringUtils.length(configContent),
                getValueHash(configContent),
                //getValueHash(entry.getOldValue()),
                getBeanName(refreshableConfiguration));

        } else {
            log.debug("Ignored config update event: [key = {}, evtType = {}, configSize = {} in bean [{}]",
                      entryKey,
                      //entry.getEventType(),
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
