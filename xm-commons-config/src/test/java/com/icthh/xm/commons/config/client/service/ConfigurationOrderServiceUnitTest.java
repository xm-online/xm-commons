package com.icthh.xm.commons.config.client.service;

import com.icthh.xm.commons.config.domain.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ConfigurationOrderServiceUnitTest {

    private static final String XM_ORDER_PATH = "/config/tenants/XM/order.yml";
    private static final String XM_ORDER_CONTENT = """
        order:
          - tenant-config.yml
          - entity/xmentityspec/*.yml
          - lep/**
        """;

    private ConfigurationOrderService service;

    @Before
    public void setUp() {
        service = new ConfigurationOrderService();
    }

    private void seedXmOrder() {
        service.processOrderConfigs(Map.of(XM_ORDER_PATH, new Configuration(XM_ORDER_PATH, XM_ORDER_CONTENT)));
    }

    @Test
    public void keepsOriginalOrderWhenNoOrderConfig() {
        List<String> paths = List.of(
            "/config/tenants/XM/webapp/settings-public.yml",
            "/config/tenants/XM/tenant-config.yml");

        assertEquals(paths, service.sortPaths(paths));
    }

    @Test
    public void sortsByPatternsWithOrderFileFirstAndUnmatchedLast() {
        seedXmOrder();
        List<String> paths = List.of(
            "/config/tenants/XM/lep/some/Script$$around.groovy",
            "/config/tenants/XM/webapp/settings-public.yml",
            "/config/tenants/XM/entity/xmentityspec/specs.yml",
            XM_ORDER_PATH,
            "/config/tenants/XM/tenant-config.yml");

        List<String> expected = List.of(
            XM_ORDER_PATH,
            "/config/tenants/XM/tenant-config.yml",
            "/config/tenants/XM/entity/xmentityspec/specs.yml",
            "/config/tenants/XM/lep/some/Script$$around.groovy",
            "/config/tenants/XM/webapp/settings-public.yml");

        assertEquals(expected, service.sortPaths(paths));
    }

    @Test
    public void sortIsStableWithinSameRank() {
        seedXmOrder();
        List<String> paths = List.of(
            "/config/tenants/XM/lep/b/Second.groovy",
            "/config/tenants/XM/lep/a/First.groovy",
            "/config/tenants/XM/webapp/z.yml",
            "/config/tenants/XM/webapp/a.yml");

        assertEquals(paths, service.sortPaths(paths));
    }

    @Test
    public void keepsOtherTenantsAndNonTenantPathsInPlace() {
        seedXmOrder();
        List<String> paths = List.of(
            "/config/tenants/TEST/a.yml",
            "/config/tenants/XM/webapp/settings-public.yml",
            "/config/tenants/TEST/b.yml",
            "/config/tenants/XM/tenant-config.yml",
            "/some/other/path.yml");

        List<String> expected = List.of(
            "/config/tenants/TEST/a.yml",
            "/config/tenants/XM/tenant-config.yml",
            "/config/tenants/TEST/b.yml",
            "/config/tenants/XM/webapp/settings-public.yml",
            "/some/other/path.yml");

        assertEquals(expected, service.sortPaths(paths));
    }

    @Test
    public void invalidYamlKeepsPreviousOrder() {
        seedXmOrder();
        service.processOrderConfigs(Map.of(XM_ORDER_PATH, new Configuration(XM_ORDER_PATH, "{{{ not valid yaml")));

        List<String> paths = List.of(
            "/config/tenants/XM/webapp/settings-public.yml",
            "/config/tenants/XM/tenant-config.yml");

        List<String> expected = List.of(
            "/config/tenants/XM/tenant-config.yml",
            "/config/tenants/XM/webapp/settings-public.yml");

        assertEquals(expected, service.sortPaths(paths));
    }

    @Test
    public void deletedOrderFileRemovesOrdering() {
        seedXmOrder();
        service.processOrderConfigs(Map.of(XM_ORDER_PATH, new Configuration(XM_ORDER_PATH, null)));

        List<String> paths = List.of(
            "/config/tenants/XM/webapp/settings-public.yml",
            "/config/tenants/XM/tenant-config.yml");

        assertEquals(paths, service.sortPaths(paths));
    }

    @Test
    public void emptyContentOrderFileRemovesOrdering() {
        seedXmOrder();
        service.processOrderConfigs(Map.of(XM_ORDER_PATH, new Configuration(XM_ORDER_PATH, "")));

        List<String> paths = List.of(
            "/config/tenants/XM/webapp/settings-public.yml",
            "/config/tenants/XM/tenant-config.yml");

        assertEquals(paths, service.sortPaths(paths));
    }

    @Test
    public void ignoresUnknownTopLevelKeys() {
        String content = """
            someFutureKey: true
            order:
              - tenant-config.yml
            """;
        service.processOrderConfigs(Map.of(XM_ORDER_PATH, new Configuration(XM_ORDER_PATH, content)));

        List<String> paths = List.of(
            "/config/tenants/XM/webapp/settings-public.yml",
            "/config/tenants/XM/tenant-config.yml");

        List<String> expected = List.of(
            "/config/tenants/XM/tenant-config.yml",
            "/config/tenants/XM/webapp/settings-public.yml");

        assertEquals(expected, service.sortPaths(paths));
    }

    @Test
    public void normalizesLeadingSlashInPatterns() {
        String content = "order:\n  - /tenant-config.yml\n";
        service.processOrderConfigs(Map.of(XM_ORDER_PATH, new Configuration(XM_ORDER_PATH, content)));

        List<String> paths = List.of(
            "/config/tenants/XM/webapp/settings-public.yml",
            "/config/tenants/XM/tenant-config.yml");

        List<String> expected = List.of(
            "/config/tenants/XM/tenant-config.yml",
            "/config/tenants/XM/webapp/settings-public.yml");

        assertEquals(expected, service.sortPaths(paths));
    }

    @Test
    public void bareTenantRootPathDoesNotThrowAndIsUnmatched() {
        seedXmOrder();
        String tenantRootPath = "/config/tenants/XM";
        List<String> paths = List.of(
            tenantRootPath,
            "/config/tenants/XM/tenant-config.yml",
            "/config/tenants/XM/webapp/settings-public.yml");

        List<String> expected = List.of(
            "/config/tenants/XM/tenant-config.yml",
            tenantRootPath,
            "/config/tenants/XM/webapp/settings-public.yml");

        assertEquals(expected, service.sortPaths(paths));
    }

    @Test
    public void commonsIsTreatedLikeAnyTenant() {
        String commonsOrderPath = "/config/tenants/commons/order.yml";
        service.processOrderConfigs(Map.of(commonsOrderPath,
            new Configuration(commonsOrderPath, "order:\n  - lep/**\n")));

        List<String> paths = List.of(
            "/config/tenants/commons/other.yml",
            "/config/tenants/commons/lep/Script$$tenant.groovy");

        List<String> expected = List.of(
            "/config/tenants/commons/lep/Script$$tenant.groovy",
            "/config/tenants/commons/other.yml");

        assertEquals(expected, service.sortPaths(paths));
    }
}
