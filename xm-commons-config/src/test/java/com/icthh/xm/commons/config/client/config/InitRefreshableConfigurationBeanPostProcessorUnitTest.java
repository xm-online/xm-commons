package com.icthh.xm.commons.config.client.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.api.FetchConfigurationSettings;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.applied.AppliedConfigService;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.service.CommonConfigService;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.SneakyThrows;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class InitRefreshableConfigurationBeanPostProcessorUnitTest {

    @Mock
    private RefreshableConfiguration refreshableConfiguration;
    @Mock
    private CommonConfigRepository commonConfigRepository;
    @Mock
    private ObjectProvider<ConfigService> configServiceProvider;
    private ConfigService configService;

    private FetchConfigurationSettings fetchConfigurationSettings;

    @Spy
    private XmConfigProperties configProperties;
    private InitRefreshableConfigurationBeanPostProcessor processor;
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
        processor = new InitRefreshableConfigurationBeanPostProcessor(configServiceProvider, configProperties, fetchConfigurationSettings);

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
        processor = new InitRefreshableConfigurationBeanPostProcessor(configServiceProvider, configProperties, fetchConfigurationSettings);

        List<String> configs = processor.initConfigPaths(refreshableConfiguration, configMap);

        assertListsEquals(new ArrayList<>(configMap.keySet()), configs);
    }

    @Test
    public void shouldContainAllTenantsIfIncludePropertyEmptyDuringUpdate() {
        when(configService.getConfigMapAntPattern(any(), any())).thenReturn(configMap);
        when(configServiceProvider.getIfAvailable()).thenReturn(configService);

        processor = new InitRefreshableConfigurationBeanPostProcessor(configServiceProvider, configProperties, fetchConfigurationSettings);
        processor.postProcessBeforeInitialization(refreshableConfiguration, "refreshableConfiguration");
        processor.postProcessAfterInitialization(refreshableConfiguration, "refreshableConfiguration");

        configService.updateConfigurations("commit", configKeys);

        configKeys.forEach(configKey -> verify(refreshableConfiguration).onRefresh(eq(configKey), any()));

        verify(refreshableConfiguration).refreshFinished(eq(configKeys));

    }

    @Test
    public void shouldContainIncludedTenantsAndCommonsDuringUpdate() {
        when(configProperties.getIncludeTenants()).thenReturn(Set.of("tenant1", "Tenant2"));
        when(configService.getConfigMapAntPattern(any(), any())).thenReturn(configMap);
        when(configServiceProvider.getIfAvailable()).thenReturn(configService);
        processor = new InitRefreshableConfigurationBeanPostProcessor(configServiceProvider, configProperties, fetchConfigurationSettings);
        processor.postProcessBeforeInitialization(refreshableConfiguration, "refreshableConfiguration");
        processor.postProcessAfterInitialization(refreshableConfiguration, "refreshableConfiguration");

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

        processor = new InitRefreshableConfigurationBeanPostProcessor(configServiceProvider, configProperties, fetchConfigurationSettings);
        processor.postProcessBeforeInitialization(refreshableConfiguration, "refreshableConfiguration");
        processor.postProcessAfterInitialization(refreshableConfiguration, "refreshableConfiguration");
    }

    private static <T extends Comparable<T>> boolean assertListsEquals(Collection<T> expected, Collection<T> actual) {
        ArrayList<T> mutableExpected = new ArrayList<>(expected);
        ArrayList<T> mutableActual = new ArrayList<>(actual);

        Collections.sort(mutableExpected);
        Collections.sort(mutableActual);
        Assert.assertEquals(mutableExpected, mutableActual);
        return true;
    }

    @Test
    public void appliedConfigServiceGetsVersionBeforeConfigLoadAndSeedsHashes() {
        ConfigService service = mock(ConfigService.class);
        when(configServiceProvider.getIfAvailable()).thenReturn(service);
        when(service.getConfigMapAntPattern(any(), any())).thenReturn(Map.of("/config/tenants/XM/a.yml",
            new Configuration("/config/tenants/XM/a.yml", "a")));
        AppliedConfigService applied = mock(AppliedConfigService.class);
        processor = processorWith(applied);

        processor.postProcessBeforeInitialization(refreshableConfiguration, "bean");
        processor.postProcessAfterInitialization(refreshableConfiguration, "bean");

        InOrder order = inOrder(applied, service);
        order.verify(applied).loadStartupVersion();
        order.verify(service).getConfigMapAntPattern(any(), any());
        order.verify(applied).onInitialLoad(any());
    }

    @Test
    public void startupWithEmptyVersionKeepsUnknownAndInitCompletes() {
        when(configServiceProvider.getIfAvailable()).thenReturn(configService);
        when(commonConfigRepository.getVersion()).thenReturn(Optional.empty());
        when(commonConfigRepository.getConfigByPatternPaths(any(), any())).thenReturn(Map.of("/config/tenants/XM/a.yml",
            new Configuration("/config/tenants/XM/a.yml", "a")));
        AppliedConfigService applied = new AppliedConfigService(commonConfigRepository, fetchConfigurationSettings, true);
        processor = processorWith(applied);

        processor.postProcessBeforeInitialization(refreshableConfiguration, "bean");
        processor.postProcessAfterInitialization(refreshableConfiguration, "bean");

        assertEquals("unknown", applied.getVersion().mainVersion());
        assertEquals(1, applied.getPathHashes().size());
        verify(refreshableConfiguration).refreshableConfigurationInited();
    }

    @SuppressWarnings("unchecked")
    private InitRefreshableConfigurationBeanPostProcessor processorWith(AppliedConfigService applied) {
        ObjectProvider<AppliedConfigService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(applied);
        return new InitRefreshableConfigurationBeanPostProcessor(configServiceProvider, configProperties,
            fetchConfigurationSettings, provider);
    }
}
