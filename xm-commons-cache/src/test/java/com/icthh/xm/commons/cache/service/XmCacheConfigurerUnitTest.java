package com.icthh.xm.commons.cache.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class XmCacheConfigurerUnitTest {

    public String demoApp = "demo-app";

    XmCacheConfigurer xmCacheConfigurer;

    ApplicationEventPublisher applicationEventPublisher;

    @Before
    public void setUp() throws Exception {
        applicationEventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        xmCacheConfigurer = new XmCacheConfigurer(demoApp, applicationEventPublisher);
    }

    @Test
    public void onRefresh() {
    }

    @Test
    public void isListeningConfigurationShouldBeTrue() {
        assertTrue(xmCacheConfigurer.isListeningConfiguration("/config/tenants/tenant/demo-app/cache.yml"));
    }

    @Test
    public void isListeningConfigurationShouldBeFalse() {
        assertFalse(xmCacheConfigurer.isListeningConfiguration("/config/tenants/tenant/demo-ap/cache.yml"));
        assertFalse(xmCacheConfigurer.isListeningConfiguration("/config/tenants/tenant/emo-app/cache.yml"));
        assertFalse(xmCacheConfigurer.isListeningConfiguration("/config/tenants/tenant/demo-app1/cache.yml"));
        assertFalse(xmCacheConfigurer.isListeningConfiguration("/config/tenants/tenant/demo-app/cach.yml"));
    }

    @Test
    public void readConfig() {
    }

    @Test
    public void extractTenant() {
        assertEquals("XXX", xmCacheConfigurer.extractTenant("/config/tenants/XXX/demo-app/cache.yml"));
    }

    @Test
    public void onRefreshEmitsConfigurationEvent() throws URISyntaxException, IOException {
        var cacheConfig = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(getClass().getResource("/cache-cfg.yml")).toURI())));
        verify(applicationEventPublisher, never()).publishEvent(eq(InitCachesEvent.class));
        xmCacheConfigurer.onRefresh("/config/tenants/XXX/" + demoApp + "/cache.yml", cacheConfig);
        verify(applicationEventPublisher, times(1)).publishEvent(argThat(event -> {
            var value = (InitCachesEvent) event;
            assertEquals("XXX", ((InitCachesEvent) event).getTenantKey());
            assertEquals(1, value.getCacheList().size());
            assertEquals("DemoCache", value.getCacheList().get(0).getCacheName());
            assertEquals(Integer.valueOf(10), value.getCacheList().get(0).getInitialCapacity());
            return true;
        }));
    }

    @Test
    public void onRefreshEmitsEmptyEventForBrokenConfiguration() throws URISyntaxException, IOException {
        var cacheConfig = "broken cfg";
        verify(applicationEventPublisher, times(0)).publishEvent(eq(InitCachesEvent.class));
        xmCacheConfigurer.onRefresh("/config/tenants/XXX/" + demoApp + "/cache.yml", cacheConfig);
        verify(applicationEventPublisher, times(1)).publishEvent(argThat(event -> {
            var value = (InitCachesEvent) event;
            assertEquals("XXX", ((InitCachesEvent) event).getTenantKey());
            assertTrue(value.getCacheList().isEmpty());
            return true;
        }));
    }

}
