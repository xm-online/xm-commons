package com.icthh.xm.commons.domain.event.config;

import com.icthh.xm.commons.domain.event.service.impl.OutboxTransport;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class XmDomainEventConfigurationTest {

    public static final String APP_NAME = "testEntity";
    public static final String TENANT = "TEST";
    public static final String UPDATE_KEY = "/config/tenants/" + TENANT + "/testEntity/domain-events.yml";
    private XmDomainEventConfiguration xmDomainEventConfiguration;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private LiquibaseRunner liquibaseRunner;


    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        when(tenantContextHolder.getTenantKey()).thenReturn(TENANT);
        xmDomainEventConfiguration = new XmDomainEventConfiguration(APP_NAME, tenantContextHolder, liquibaseRunner);
    }

    @Test
    public void shouldListenToCorrectPath() {
        String correctConfigPath = "/config/tenants/{tenant}/" + APP_NAME + "/domain-events.yml";
        boolean result = xmDomainEventConfiguration.isListeningConfiguration(correctConfigPath);
        assertTrue(result);
    }

    @Test
    public void shouldNotListenToWrongPath() {
        String correctConfigPath = "/config/tenants/{tenant}/" + APP_NAME + "/wrong-domain-events.yml";
        boolean result = xmDomainEventConfiguration.isListeningConfiguration(correctConfigPath);
        assertFalse(result);
    }

    @Test
    public void shouldInitEnabledConfig() {
        String enabledConfig = readConfigFile("/enabledDomainEvents.yml");
        xmDomainEventConfiguration.onRefresh(UPDATE_KEY, enabledConfig);
        SourceConfig db = xmDomainEventConfiguration.getInterceptorConfig("db");
        assertNotNull(db);
        verify(liquibaseRunner, times(1)).runOnTenant(TENANT);
    }

    @Test
    public void shouldNotInitDisabledConfig() {
        String enabledConfig = readConfigFile("/disabledDomainEvents.yml");
        xmDomainEventConfiguration.onRefresh(UPDATE_KEY, enabledConfig);
        Exception exception = null;
        try {
            xmDomainEventConfiguration.getInterceptorConfig("db");
        } catch (Exception e) {
            exception = e;
        }
        assertEquals(IllegalStateException.class, exception.getClass());
        verify(liquibaseRunner, times(0)).runOnTenant(TENANT);
    }

    @Test
    public void shouldInitConfig() {
        String enabledConfig = readConfigFile("/enabledDomainEvents.yml");
        xmDomainEventConfiguration.onRefresh(UPDATE_KEY, enabledConfig);
        SourceConfig dbSourceConfig = xmDomainEventConfiguration.getInterceptorConfig("db");
        assertNotNull(dbSourceConfig);
        assertEquals(OutboxTransport.class, dbSourceConfig.getTransport());
        assertTrue(dbSourceConfig.isEnabled());

        SourceConfig webSourceConfig = xmDomainEventConfiguration.getInterceptorConfig("web");
        assertNotNull(webSourceConfig);
        assertEquals(OutboxTransport.class, webSourceConfig.getTransport());
        assertFalse(webSourceConfig.isEnabled());

        SourceConfig nonExistentConfig = xmDomainEventConfiguration.getInterceptorConfig("nonExistentConfig");
        assertNull(nonExistentConfig);
    }

    private String readConfigFile(String path) {
        return new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(path)))
            .lines().collect(Collectors.joining("\n"));
    }
}
