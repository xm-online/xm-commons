package com.icthh.xm.commons.config.client.config;

import static com.icthh.xm.commons.config.client.config.XmConfigHazelcastConfiguration.TENANT_CONFIGURATION_HAZELCAST;
import static com.icthh.xm.commons.config.client.config.XmConfigHazelcastConfiguration.TENANT_CONFIGURATION_MAP;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty("xm-config.enabled")
public class InitRefreshableConfigurationBeanPostProcessor implements BeanPostProcessor {

    public static final String LOG_CONFIG_EMPTY = "<CONFIG_EMPTY>";

    private final HazelcastInstance hazelcastInstance;

    private final Map<String, RefreshableConfiguration> refreshableConfigurations = new HashMap<>();

    public InitRefreshableConfigurationBeanPostProcessor(
        @Qualifier(TENANT_CONFIGURATION_HAZELCAST) HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

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
        IMap<String, String> configMap = hazelcastInstance.getMap(TENANT_CONFIGURATION_MAP);

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

        final boolean includeValue = true;
        configMap.addEntryListener((EntryAddedListener<String, String>) e -> {
            onEntryChange(refreshableConfiguration, e, configMap);
        }, includeValue);
        configMap.addEntryListener((EntryRemovedListener<String, String>) e -> {
            onEntryChange(refreshableConfiguration, e, configMap);
        }, includeValue);
        configMap.addEntryListener((EntryUpdatedListener<String, String>) e -> {
            onEntryChange(refreshableConfiguration, e, configMap);
        }, includeValue);
    }

    private void onEntryChange(RefreshableConfiguration refreshableConfiguration,
                               EntryEvent<String, String> entry,
                               IMap<String, String> configMap) {

        String entryKey = entry.getKey();
        String configContent = configMap.get(entryKey);

        if (refreshableConfiguration.isListeningConfiguration(entryKey)) {

            refreshableConfiguration.onRefresh(entryKey, configContent);

            log.info(
                "Process config update event: "
                + "[key = {}, evtType = {}, size = {}, newHash = {}, oldHash = {}] in bean: [{}]",
                entryKey,
                entry.getEventType(),
                StringUtils.length(configContent),
                getValueHash(configContent),
                getValueHash(entry.getOldValue()),
                getBeanName(refreshableConfiguration));

        } else {
            log.debug("Ignored config update event: [key = {}, evtType = {}, configSize = {} in bean [{}]",
                      entryKey,
                      entry.getEventType(),
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
