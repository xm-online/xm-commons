package com.icthh.xm.commons.config.client.service;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.service.ConfigCacheFactory.ConfigCache;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.function.Supplier;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigCacheTest {

    private Supplier<String> mockFunction = mock(Supplier.class);
    private RefreshableConfiguration refreshableConfiguration = mock(RefreshableConfiguration.class);
    private ConfigCacheFactory factory = new ConfigCacheFactory(singletonList(refreshableConfiguration));

    @Test
    public void cacheConfigTest() {
        ConfigCache<String> configCache = factory.create(singletonList(refreshableConfiguration.getClass()));
        when(mockFunction.get()).thenReturn("mockValue");
        assertEquals("mockValue", configCache.withCache("test", () -> mockFunction.get()));
        assertEquals("mockValue", configCache.withCache("test", () -> mockFunction.get()));
        verify(mockFunction).get();
    }

    @Test
    public void cacheConfigUpdateConfigTest() {
        when(refreshableConfiguration.isListeningConfiguration("mockKey")).thenReturn(true);
        ConfigCache<String> configCache = factory.create(singletonList(refreshableConfiguration.getClass()));
        when(mockFunction.get()).thenReturn("mockValue");
        assertEquals("mockValue", configCache.withCache("test", () -> mockFunction.get()));
        assertEquals("mockValue", configCache.withCache("test", () -> mockFunction.get()));
        factory.onRefresh("mockKey", "mockConfig");
        assertEquals("mockValue", configCache.withCache("test", () -> mockFunction.get()));
        verify(mockFunction, times(2)).get();
    }

    @Test
    public void cacheConfigWithDifferentKeysConfigTest() {
        ConfigCache<String> configCache = factory.create(singletonList(refreshableConfiguration.getClass()));
        when(mockFunction.get()).thenReturn("mockValue");
        assertEquals("mockValue", configCache.withCache("test", () -> mockFunction.get()));
        assertEquals("mockValue", configCache.withCache("test1", () -> mockFunction.get()));
        verify(mockFunction, times(2)).get();
    }
}
