package com.icthh.xm.commons.config.client.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class InitRefreshableConfigurationBeanPostProcessorUnitTest {

    @Mock
    private RefreshableConfiguration refreshableConfiguration;
    @Mock
    private ConfigService configService;
    @Spy
    private XmConfigProperties configProperties;
    private InitRefreshableConfigurationBeanPostProcessor processor;
    private Map<String, Configuration> configMap;

    @Before
    @SneakyThrows
    public void init() {
        when(refreshableConfiguration.isListeningConfiguration(anyString())).thenReturn(true);

        configMap = Map.of(
            "/config/tenants/TENANT1/dashboard/dashboards/ADMIN_METRICS-27.yml",
            Configuration.of().build(),
            "/config/tenants/TENANT2/custom-privileges.yml",
            Configuration.of().build(),
            "/config/tenants/TENANT3/entity/lep/function/v1/otp/Function$$GET_OTP$$tenant.groovy",
            Configuration.of().build(),
            "/config/tenants/TENANT1/uaa/uaa.yml",
            Configuration.of().build(),
            "/config/tenants/TENANT3/dashboard/dashboards/ADMIN-180.yml",
            Configuration.of().build(),
            "/config/tenants/TENANT3/entity/lep/service/entity/Save$$PC_RULES$DISABLEOFFER$$around.groovy",
            Configuration.of().build(),
            "/config/tenants/TENANT4/roles.yml",
            Configuration.of().build(),
            "/config/tenants/commons/lep/entity/MathService$$tenant.groovy",
            Configuration.of().build(),
            "/.idea/httpRequests/http-requests-log.http",
            Configuration.of().build()
        );

    }

    @Test
    public void shouldContainIncludedTenantsAndCommons() {

        when(configProperties.getIncludeTenants()).thenReturn(Set.of("tenant1", "Tenant2"));
        processor = new InitRefreshableConfigurationBeanPostProcessor(configService, configProperties);

        List<String> configs = processor.initConfigPaths(refreshableConfiguration, configMap);

        assertListsEquals(List.of("/config/tenants/TENANT1/dashboard/dashboards/ADMIN_METRICS-27.yml",
                                  "/config/tenants/TENANT2/custom-privileges.yml",
                                  "/config/tenants/TENANT1/uaa/uaa.yml",
                                  "/config/tenants/commons/lep/entity/MathService$$tenant.groovy"
        ), configs);

    }

    @Test
    public void shouldContainAllTenantsIfIncludePropertyEmpty() {
        when(configProperties.getIncludeTenants()).thenReturn(null);
        processor = new InitRefreshableConfigurationBeanPostProcessor(configService, configProperties);

        List<String> configs = processor.initConfigPaths(refreshableConfiguration, configMap);

        assertListsEquals(new ArrayList<>(configMap.keySet()), configs);
    }

    private static <T extends Comparable<T>> void assertListsEquals(List<T> expected, List<T> actual) {
        ArrayList<T> mutableExpected = new ArrayList<>(expected);
        ArrayList<T> mutableActual = new ArrayList<>(actual);

        Collections.sort(mutableExpected);
        Collections.sort(mutableActual);
        Assert.assertEquals(mutableExpected, mutableActual);
    }
}
