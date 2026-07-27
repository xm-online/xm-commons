package com.icthh.xm.commons.config.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.api.ConfigurationChangedListener;
import com.icthh.xm.commons.config.client.api.FetchConfigurationSettings;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class CommonConfigServiceUnitTest {

    private CommonConfigService configService;
    @Mock
    private CommonConfigRepository commonConfigRepository;

    @Before
    public void setUp() {
        FetchConfigurationSettings fetchConfigurationSettings = new FetchConfigurationSettings("test", true);
        configService = new CommonConfigService(fetchConfigurationSettings, commonConfigRepository, new ConfigurationOrderService());
    }

    @Test
    public void getConfigurationMap() {
        Map<String, Configuration> config = Collections.singletonMap("path", new Configuration("path", "content"));
        when(commonConfigRepository.getConfig("commit")).thenReturn(config);

        assertThat(configService.getConfigurationMap("commit")).isEqualTo(config);
    }

    @Test
    public void updateConfigurations() {
        Map<String, Configuration> config = Collections.singletonMap("path", new Configuration("path", "content"));
        when(commonConfigRepository.getConfig(eq("commit"), anyList())).thenReturn(config);

        List<ConfigurationChangedListener> configurationListeners = new ArrayList<>();
        configurationListeners.add(mock(ConfigurationChangedListener.class));

        configurationListeners.forEach(configService::addConfigurationChangedListener);
        configService.updateConfigurations("commit", Collections.singletonList("path"));

        configurationListeners.forEach(configurationListener ->
                                           verify(configurationListener)
                                               .onConfigurationChanged(refEq(config.get("path"))));
        configurationListeners.forEach(configurationListener ->
                verify(configurationListener).refreshFinished(Collections.singletonList("path")));
    }

    @Test
    public void updateConfigurationsWhenFetchAllFalseAndPathNotMatch() {
        FetchConfigurationSettings fetchConfigurationSettings = new FetchConfigurationSettings("test", false);
        configService = spy(new CommonConfigService(fetchConfigurationSettings, commonConfigRepository, new ConfigurationOrderService()));

        List<String> testPaths = Collections.singletonList("path");
        List<ConfigurationChangedListener> configurationListeners = new ArrayList<>();
        configurationListeners.add(mock(ConfigurationChangedListener.class));

        configurationListeners.forEach(configService::addConfigurationChangedListener);
        configService.updateConfigurations("commit", testPaths);

        verify(configService, never()).getConfigurationMap(eq("commit"), eq(testPaths));
    }

    @Test
    public void updateConfigurationsWhenFetchAllFalseAndPathsHasMatch() {
        FetchConfigurationSettings fetchConfigurationSettings = spy(new FetchConfigurationSettings("test", false));
        CommonConfigService configService = spy(new CommonConfigService(fetchConfigurationSettings, commonConfigRepository, new ConfigurationOrderService()));

        when(fetchConfigurationSettings.getMsConfigPatterns()).thenReturn(List.of(
                "/config/tenants/commons/**",
                "/config/tenants/*",
                "/config/tenants/{tenantName}/commons/**",
                "/config/tenants/{tenantName}/*",
                "/config/tenants/{tenantName}/" + "test" + "/**",
                "/config/tenants/{tenantName}/config/**"));

        List<String> testPaths = List.of(
                "/config/tenants/commons/test.txt",
                "/config/tenants/test.txt",
                "/config/tenants/XM/commons/commons-file.txt",
                "/config/tenants/XM/simple-file.txt",
                "/config/tenants/XM/demo/demo-file.txt", //not
                "/config/tenants/XM/test/test2.txt",
                "/config/tenants/XM/config/config.txt"
        );

        List<String> expectedPaths = List.of(
                "/config/tenants/commons/test.txt",
                "/config/tenants/test.txt",
                "/config/tenants/XM/commons/commons-file.txt",
                "/config/tenants/XM/simple-file.txt",
                "/config/tenants/XM/test/test2.txt",
                "/config/tenants/XM/config/config.txt"
        );

        Map<String, Configuration> config = Map.of(
                "/config/tenants/commons/test.txt", new Configuration("/config/tenants/commons/test.txt", "content text"),
                "/config/tenants/test.txt", new Configuration("/config/tenants/test.txt", "content text"),
                "/config/tenants/XM/commons/commons-file.txt", new Configuration("/config/tenants/XM/commons/commons-file.txt", "content text"),
                "/config/tenants/XM/simple-file.txt", new Configuration("/config/tenants/XM/simple-file.txt", "content text"),
                "/config/tenants/XM/test/test2.txt", new Configuration("/config/tenants/XM/test/test2.txt", "content text"),
                "/config/tenants/XM/config/config.txt", new Configuration("/config/tenants/XM/config/config.txt", "content text")
        );
        when(commonConfigRepository.getConfig(eq("commit"), eq(expectedPaths))).thenReturn(config);

        List<ConfigurationChangedListener> configurationListeners = new ArrayList<>();
        configurationListeners.add(mock(ConfigurationChangedListener.class));

        configurationListeners.forEach(configService::addConfigurationChangedListener);
        configService.updateConfigurations("commit", testPaths);

        verify(configService).getConfigurationMap(eq("commit"), eq(expectedPaths));
        assertEquals(config, configService.getConfigurationMap("commit", expectedPaths));
    }

    @Test
    public void updateMultiplyConfigurationsWithSingleFinished() {
        Map<String, Configuration> config = Map.of(
                "path1", new Configuration("path1", "content1"),
                "path2", new Configuration("path2", "content2")
        );
        when(commonConfigRepository.getConfig(eq("commit"), anyList())).thenReturn(config);

        List<ConfigurationChangedListener> configurationListeners = new ArrayList<>();
        configurationListeners.add(mock(ConfigurationChangedListener.class));

        configurationListeners.forEach(configService::addConfigurationChangedListener);
        configService.updateConfigurations("commit", List.of("path1", "path2"));

        configurationListeners.forEach(configurationListener ->
                verify(configurationListener)
                        .onConfigurationChanged(refEq(config.get("path1"))));
        configurationListeners.forEach(configurationListener ->
                verify(configurationListener)
                        .onConfigurationChanged(refEq(config.get("path2"))));

        configurationListeners.forEach(configurationListener ->
                verify(configurationListener).refreshFinished(List.of("path1", "path2")));
    }

    @Test
    public void updateConfigurationsWithNullContent() {
        Map<String, Configuration> config = Collections.singletonMap("path", null);
        when(commonConfigRepository.getConfig(eq("commit"), anyList())).thenReturn(config);

        List<ConfigurationChangedListener> configurationListeners = new ArrayList<>();
        configurationListeners.add(mock(ConfigurationChangedListener.class));

        configurationListeners.forEach(configService::addConfigurationChangedListener);
        configService.updateConfigurations("commit", Collections.singletonList("path"));

        configurationListeners.forEach(configurationListener ->
                                           verify(configurationListener)
                                               .onConfigurationChanged(refEq(new Configuration("path", null))));
        configurationListeners.forEach(configurationListener ->
                verify(configurationListener).refreshFinished(Collections.singletonList("path")));
    }

    @Test
    public void updateConfigurationsDispatchesInOrderYmlOrder() {
        String orderPath = "/config/tenants/XM/order.yml";
        String orderContent = "order:\n  - tenant-config.yml\n  - entity/**\n";
        String tenantConfigPath = "/config/tenants/XM/tenant-config.yml";
        String entityPath = "/config/tenants/XM/entity/specs.yml";
        String otherPath = "/config/tenants/XM/webapp/settings.yml";

        List<String> incoming = List.of(otherPath, entityPath, tenantConfigPath, orderPath);
        Map<String, Configuration> config = Map.of(
            orderPath, new Configuration(orderPath, orderContent),
            tenantConfigPath, new Configuration(tenantConfigPath, "c1"),
            entityPath, new Configuration(entityPath, "c2"),
            otherPath, new Configuration(otherPath, "c3"));
        when(commonConfigRepository.getConfig(eq("commit"), anyList())).thenReturn(config);

        ConfigurationChangedListener listener = mock(ConfigurationChangedListener.class);
        configService.addConfigurationChangedListener(listener);

        configService.updateConfigurations("commit", incoming);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onConfigurationChanged(refEq(config.get(orderPath)));
        inOrder.verify(listener).onConfigurationChanged(refEq(config.get(tenantConfigPath)));
        inOrder.verify(listener).onConfigurationChanged(refEq(config.get(entityPath)));
        inOrder.verify(listener).onConfigurationChanged(refEq(config.get(otherPath)));
        verify(listener).refreshFinished(List.of(orderPath, tenantConfigPath, entityPath, otherPath));
    }

    @Test
    public void orderFromPreviousBatchAppliesToNextBatch() {
        String orderPath = "/config/tenants/XM/order.yml";
        Map<String, Configuration> firstBatch = Map.of(orderPath,
            new Configuration(orderPath, "order:\n  - tenant-config.yml\n"));
        when(commonConfigRepository.getConfig(eq("commit1"), anyList())).thenReturn(firstBatch);
        configService.updateConfigurations("commit1", List.of(orderPath));

        String tenantConfigPath = "/config/tenants/XM/tenant-config.yml";
        String otherPath = "/config/tenants/XM/webapp/settings.yml";
        Map<String, Configuration> secondBatch = Map.of(
            tenantConfigPath, new Configuration(tenantConfigPath, "c1"),
            otherPath, new Configuration(otherPath, "c2"));
        when(commonConfigRepository.getConfig(eq("commit2"), anyList())).thenReturn(secondBatch);

        ConfigurationChangedListener listener = mock(ConfigurationChangedListener.class);
        configService.addConfigurationChangedListener(listener);
        configService.updateConfigurations("commit2", List.of(otherPath, tenantConfigPath));

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onConfigurationChanged(refEq(secondBatch.get(tenantConfigPath)));
        inOrder.verify(listener).onConfigurationChanged(refEq(secondBatch.get(otherPath)));
        verify(listener).refreshFinished(List.of(tenantConfigPath, otherPath));
    }

    @Test
    public void deletedOrderYmlOmittedFromRepositoryMapStillClearsOrdering() {
        // batch 1: order.yml with content seeds ordering for tenant XM (tenant-config.yml delivered first)
        String orderPath = "/config/tenants/XM/order.yml";
        String tenantConfigPath = "/config/tenants/XM/tenant-config.yml";
        String otherPath = "/config/tenants/XM/webapp/settings.yml";

        Map<String, Configuration> firstBatch = Collections.singletonMap(orderPath,
            new Configuration(orderPath, "order:\n  - tenant-config.yml\n"));
        when(commonConfigRepository.getConfig(eq("commit1"), anyList())).thenReturn(firstBatch);
        configService.updateConfigurations("commit1", List.of(orderPath));

        // batch 2: order.yml is deleted. Production shape (XmMsConfigCommonConfigRepository): the deleted
        // path is still dispatched (present in the incoming paths / commit) but ABSENT from the fetched
        // map (getNonNullConfiguration fabricates the null Configuration for it). Without the paths-aware
        // overload, this deletion would never reach updateOrder and the stale XM order would keep applying.
        Map<String, Configuration> secondBatch = Map.of(
            tenantConfigPath, new Configuration(tenantConfigPath, "c1"),
            otherPath, new Configuration(otherPath, "c2"));
        when(commonConfigRepository.getConfig(eq("commit2"), anyList())).thenReturn(secondBatch);

        ConfigurationChangedListener listener = mock(ConfigurationChangedListener.class);
        configService.addConfigurationChangedListener(listener);
        configService.updateConfigurations("commit2", List.of(otherPath, tenantConfigPath, orderPath));

        // ordering must no longer apply: original incoming order is preserved (no reorder to
        // [orderPath, tenantConfigPath, otherPath] as the still-active stale order would have produced)
        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onConfigurationChanged(refEq(new Configuration(otherPath, "c2")));
        inOrder.verify(listener).onConfigurationChanged(refEq(new Configuration(tenantConfigPath, "c1")));
        inOrder.verify(listener).onConfigurationChanged(refEq(new Configuration(orderPath, null)));
        verify(listener).refreshFinished(List.of(otherPath, tenantConfigPath, orderPath));
    }

}
