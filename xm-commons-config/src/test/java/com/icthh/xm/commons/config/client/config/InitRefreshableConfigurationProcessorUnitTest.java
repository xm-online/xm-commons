package com.icthh.xm.commons.config.client.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.api.FetchConfigurationSettings;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.service.CommonConfigService;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class InitRefreshableConfigurationProcessorUnitTest {

    @Mock
    private RefreshableConfiguration refreshableConfiguration;
    @Mock
    private CommonConfigRepository commonConfigRepository;
    @Mock
    private ObjectProvider<ConfigService> configServiceProvider;
    @Mock
    private ObjectProvider<LepContextRunner> lepContextRunnerProvider;
    @Mock
    private LepContextRunner lepContextRunner;

    private ConfigService configService;

    private FetchConfigurationSettings fetchConfigurationSettings;

    @Spy
    private XmConfigProperties configProperties;
    private InitRefreshableConfigurationProcessor processor;
    private Map<String, Configuration> configMap;
    private List<String> configKeys;

    @Before
    @SneakyThrows
    public void init() {
        when(refreshableConfiguration.isListeningConfiguration(anyString())).thenReturn(true);
        fetchConfigurationSettings = new FetchConfigurationSettings("test", true);
        configService = new CommonConfigService(fetchConfigurationSettings, commonConfigRepository);

        configKeys = List.of(
            "/config/tenants/TENANT1/dashboard/dashboards/ADMIN_METRICS-27.yml",
            "/config/tenants/TENANT2/custom-privileges.yml",
            "/config/tenants/TENANT3/entity/lep/function/v1/otp/Function$$GET_OTP$$tenant.groovy",
            "/config/tenants/TENANT1/uaa/uaa.yml",
            "/config/tenants/TENANT3/dashboard/dashboards/ADMIN-180.yml",
            "/config/tenants/TENANT3/entity/lep/service/entity/Save$$PC_RULES$DISABLEOFFER$$around.groovy",
            "/config/tenants/TENANT4/roles.yml",
            "/config/tenants/commons/lep/entity/MathService$$tenant.groovy",
            "/.idea/httpRequests/http-requests-log.http"
        );
        configMap = Map.of(
            configKeys.get(0), Configuration.of().build(),
            configKeys.get(1), Configuration.of().build(),
            configKeys.get(2), Configuration.of().build(),
            configKeys.get(3), Configuration.of().build(),
            configKeys.get(4), Configuration.of().build(),
            configKeys.get(5), Configuration.of().build(),
            configKeys.get(6), Configuration.of().build(),
            configKeys.get(7), Configuration.of().build(),
            configKeys.get(8), Configuration.of().build()
        );

    }

    @Test
    public void shouldContainIncludedTenantsAndCommons() {
        when(configProperties.getIncludeTenants()).thenReturn(Set.of("tenant1", "Tenant2"));
        processor = createProcessor();

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
        processor = createProcessor();

        List<String> configs = processor.initConfigPaths(refreshableConfiguration, configMap);

        assertListsEquals(new ArrayList<>(configMap.keySet()), configs);
    }

    @Test
    public void shouldContainAllTenantsIfIncludePropertyEmptyDuringUpdate() {
        processor = createProcessor();

        configService.updateConfigurations("commit", configKeys);

        configKeys.forEach(configKey -> verify(refreshableConfiguration).onRefresh(eq(configKey), any()));

        verify(refreshableConfiguration).refreshFinished(eq(configKeys));
    }

    @Test
    public void shouldContainIncludedTenantsAndCommonsDuringUpdate() {
        when(configProperties.getIncludeTenants()).thenReturn(Set.of("tenant1", "Tenant2"));

        processor = createProcessor();

        configService.updateConfigurations("commit", configKeys);

        var expectedUpdatedKeys = List.of("/config/tenants/TENANT1/dashboard/dashboards/ADMIN_METRICS-27.yml",
                                          "/config/tenants/TENANT2/custom-privileges.yml",
                                          "/config/tenants/TENANT1/uaa/uaa.yml",
                                          "/config/tenants/commons/lep/entity/MathService$$tenant.groovy");

        configKeys.forEach(configKey -> {
            if (expectedUpdatedKeys.contains(configKey)) {
                verify(refreshableConfiguration, times(1)).onRefresh(eq(configKey), any());
            } else {
                verify(refreshableConfiguration, times(0)).onRefresh(eq(configKey), any());
            }
        });

        verify(refreshableConfiguration, times(2))
            .refreshFinished(argThat(argument -> assertListsEquals(argument, expectedUpdatedKeys)));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenConfigServiceIsNotAvailable() {
        when(configServiceProvider.getIfAvailable()).thenReturn(null);

        new InitRefreshableConfigurationProcessor(
            configServiceProvider,
            configProperties,
            fetchConfigurationSettings,
            List.of(refreshableConfiguration),
            lepContextRunnerProvider
        );
    }

    @Test
    public void shouldInitUsingLepContextWhenRunnerIsReady() {
        when(configServiceProvider.getIfAvailable()).thenReturn(configService);
        when(configService.getConfigMapAntPattern(any(), any())).thenReturn(configMap);

        when(lepContextRunnerProvider.getIfAvailable()).thenReturn(lepContextRunner);
        when(lepContextRunner.isReady()).thenReturn(true);

        new InitRefreshableConfigurationProcessor(
            configServiceProvider,
            configProperties,
            fetchConfigurationSettings,
            List.of(refreshableConfiguration),
            lepContextRunnerProvider
        );

        verify(lepContextRunner).runInContext(any());
    }

    @Test
    public void shouldInitWithoutLepContextWhenRunnerIsNotReady() {
        when(configServiceProvider.getIfAvailable()).thenReturn(configService);
        when(configService.getConfigMapAntPattern(any(), any())).thenReturn(configMap);

        when(lepContextRunnerProvider.getIfAvailable()).thenReturn(lepContextRunner);
        when(lepContextRunner.isReady()).thenReturn(false);

        new InitRefreshableConfigurationProcessor(
            configServiceProvider,
            configProperties,
            fetchConfigurationSettings,
            List.of(refreshableConfiguration),
            lepContextRunnerProvider
        );

        verify(refreshableConfiguration).refreshableConfigurationInited();
        verify(lepContextRunner, never()).runInContext(any());
        configKeys.forEach(key ->
            verify(refreshableConfiguration).onInit(eq(key), any())
        );
        verify(refreshableConfiguration, times(configKeys.size())).onInit(anyString(), any());
    }

    private InitRefreshableConfigurationProcessor createProcessor() {
        when(configServiceProvider.getIfAvailable()).thenReturn(configService);
        when(configService.getConfigMapAntPattern(any(), any())).thenReturn(configMap);

        when(lepContextRunnerProvider.getIfAvailable()).thenReturn(null);

        return new InitRefreshableConfigurationProcessor(
            configServiceProvider,
            configProperties,
            fetchConfigurationSettings,
            List.of(refreshableConfiguration),
            lepContextRunnerProvider
        );
    }

    private static <T extends Comparable<T>> boolean assertListsEquals(Collection<T> expected, Collection<T> actual) {
        ArrayList<T> mutableExpected = new ArrayList<>(expected);
        ArrayList<T> mutableActual = new ArrayList<>(actual);

        Collections.sort(mutableExpected);
        Collections.sort(mutableActual);
        Assert.assertEquals(mutableExpected, mutableActual);
        return true;
    }
}
