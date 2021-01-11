package com.icthh.xm.commons.config.client.service;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;

@Service
public class ConfigCacheFactory implements RefreshableConfiguration {

    private final Map<Class<? extends RefreshableConfiguration>, RefreshableConfiguration> refreshableConfigurations;
    private final List<ConfigCache<?>> caches = new ArrayList<>();

    public ConfigCacheFactory(List<RefreshableConfiguration> refreshableConfigurationList) {
        Map<Class<? extends RefreshableConfiguration>, RefreshableConfiguration> refreshableConfigurations = new HashMap<>();
        refreshableConfigurationList.forEach(it -> refreshableConfigurations.put(it.getClass(), it));
        this.refreshableConfigurations = unmodifiableMap(refreshableConfigurations);
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        caches.forEach(it -> it.updateConfig(updatedKey));
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return true;
    }

    @Override
    public void onInit(String configKey, String configValue) {
        onRefresh(configKey, configValue);
    }

    public <T> ConfigCache<T> create(List<Class<? extends RefreshableConfiguration>> depends) {
        List<RefreshableConfiguration> configurations = depends.stream().map(refreshableConfigurations::get).collect(toList());
        ConfigCache<T> cache = new ConfigCache<>(configurations);
        caches.add(cache);
        return cache;
    }

    @AllArgsConstructor
    public static class ConfigCache<T> {
        private final List<RefreshableConfiguration> depends;
        private final Map<String, T> cache = new ConcurrentHashMap<>();

        public void updateConfig(String updatedKey) {
            boolean isDepends = depends.stream().anyMatch(it -> it.isListeningConfiguration(updatedKey));
            if (isDepends) {
                invalidate();
            }
        }

        public void invalidate() {
            cache.clear();
        }

        public T withCache(String key, Supplier<T> calculation) {
            return cache.computeIfAbsent(key, k -> calculation.get());
        }
    }
}
