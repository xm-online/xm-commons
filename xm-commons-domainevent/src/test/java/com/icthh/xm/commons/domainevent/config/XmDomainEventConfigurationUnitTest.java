package com.icthh.xm.commons.domainevent.config;

import com.icthh.xm.commons.domainevent.config.event.InitSourceEventPublisher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class XmDomainEventConfigurationUnitTest {

    public static final String APP_NAME = "testEntity";
    public static final String TENANT = "TEST";
    public static final String UPDATE_KEY = "/config/tenants/" + TENANT + "/testEntity/domainevent.yml";
    private XmDomainEventConfiguration xmDomainEventConfiguration;

    @Mock
    private InitSourceEventPublisher initSourceEventPublisher;

    @Mock
    private ApplicationContext applicationContext;


    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        xmDomainEventConfiguration = new XmDomainEventConfiguration(APP_NAME,
            initSourceEventPublisher,
            applicationContext);
    }

    @Test
    public void shouldListenToCorrectPath() {
        String correctConfigPath = "/config/tenants/{tenant}/" + APP_NAME + "/domainevent.yml";
        boolean result = xmDomainEventConfiguration.isListeningConfiguration(correctConfigPath);
        assertTrue(result);
    }

    @Test
    public void shouldNotListenToWrongPath() {
        String correctConfigPath = "/config/tenants/{tenant}/" + APP_NAME + "/wrong-domainevent.yml";
        boolean result = xmDomainEventConfiguration.isListeningConfiguration(correctConfigPath);
        assertFalse(result);
    }

    @Test
    public void shouldInitEnabledConfig() {
        String enabledConfig = readConfigFile("/enabledDomainEvents.yml");
        xmDomainEventConfiguration.onRefresh(UPDATE_KEY, enabledConfig);
        DbSourceConfig db = xmDomainEventConfiguration.getDbSourceConfig(TENANT, "DB");
        assertNotNull(db);
        verify(initSourceEventPublisher, times(1)).publish(eq(TENANT), any());
    }

    @Test
    public void shouldNotInitDisabledConfig() {
        String disabledConfig = readConfigFile("/disabledDomainEvents.yml");
        xmDomainEventConfiguration.onRefresh(UPDATE_KEY, disabledConfig);
        DbSourceConfig db = xmDomainEventConfiguration.getDbSourceConfig(TENANT, "DB");
        assertNull(db);
        verify(initSourceEventPublisher, never()).publish(eq(TENANT), any());
    }

    @Test
    public void shouldReturnNull_forNotFountConfig() {
        String enabledConfig = readConfigFile("/enabledDomainEvents.yml");
        String configSource = "DB";
        xmDomainEventConfiguration.onRefresh(UPDATE_KEY, enabledConfig);
        DbSourceConfig db = xmDomainEventConfiguration.getDbSourceConfig(TENANT, configSource);
        assertNotNull(db);
        String disabledConfig = readConfigFile("/disabledDomainEvents.yml");
        xmDomainEventConfiguration.onRefresh(UPDATE_KEY, disabledConfig);

        DbSourceConfig result = xmDomainEventConfiguration.getDbSourceConfig(TENANT, configSource);
        assertNull(result);
    }

    @Test
    public void shouldInitConfig() {
        String enabledConfig = readConfigFile("/enabledDomainEvents.yml");
        xmDomainEventConfiguration.onRefresh(UPDATE_KEY, enabledConfig);
        DbSourceConfig dbSourceConfig = xmDomainEventConfiguration.getDbSourceConfig(TENANT, "DB");
        assertNotNull(dbSourceConfig);
        assertEquals("outboxTransport", dbSourceConfig.getTransport());
        assertTrue(dbSourceConfig.isEnabled());

        WebSourceConfig webSourceConfig = xmDomainEventConfiguration.getWebSourceConfig(TENANT, "WEB");
        assertNotNull(webSourceConfig);
        assertEquals("outboxTransport", webSourceConfig.getTransport());
        assertFalse(webSourceConfig.isEnabled());

        SourceConfig nonExistentConfig = xmDomainEventConfiguration.getDbSourceConfig(TENANT, "nonExistentConfig");
        assertNull(nonExistentConfig);
    }

    @Test
    public void shouldGetOperationMappingConfig() {
        String enabledConfig = readConfigFile("/mappingDomainEvents.yml");
        xmDomainEventConfiguration.onRefresh(UPDATE_KEY, enabledConfig);

        String url = APP_NAME + "/api/xm-entities/123/states/FINISH";
        String operationMapping = xmDomainEventConfiguration.getOperationMapping(TENANT, "PUT", url);
        assertNotNull(operationMapping);
        assertEquals("statechange id: 123 to: FINISH", operationMapping);
    }

    @Test
    public void shouldGetOperationWithDefaultValue() {
        String url = "/api/xm-entities/123/states/FINISH";
        String operationMapping = xmDomainEventConfiguration.getOperationMapping(TENANT, "DELETE", url);
        assertNotNull(operationMapping);
        assertEquals("xm-entities deleted", operationMapping);
    }

    private String readConfigFile(String path) {
        return new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(path)))
            .lines().collect(Collectors.joining("\n"));
    }
}
