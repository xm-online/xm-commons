package com.icthh.xm.commons.cache.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.Assert.*;

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
}
