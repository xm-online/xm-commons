package com.icthh.xm.commons.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XmCommonsApplicationPropertiesUnitTest {

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ServiceProperties extends XmCommonsApplicationProperties {
        private String specificationName;
    }

    @Test
    public void testServicePropertiesBindTheCommonsKeys() {
        Map<String, String> source = Map.ofEntries(
            Map.entry("application.specification-name", "spec"),
            Map.entry("application.config-private-key", "key"),
            Map.entry("application.api-docs.privilege-check", "true"),
            Map.entry("application.prometheus.public", "true"),
            Map.entry("application.management.access.role", "SYSTEM-ADMIN"),
            Map.entry("application.management.info.public", "true"),
            Map.entry("application.management.logs.system-admin-only", "true"),
            Map.entry("application.tenant-cache.enabled", "true"),
            Map.entry("application.tenant-cache.redis.port", ""),
            Map.entry("application.tenant-memory-cache.enabled", "true"),
            Map.entry("application.xm-auto-configuration-filter.disabled", "true")
        );

        ServiceProperties properties = new Binder(new MapConfigurationPropertySource(source))
            .bind("application", Bindable.of(ServiceProperties.class)).get();

        assertEquals("spec", properties.getSpecificationName());
        assertEquals("key", properties.getConfigPrivateKey());
        assertTrue(properties.getApiDocs().isPrivilegeCheck());
        assertTrue(properties.getPrometheus().isPublic());
        assertEquals("SYSTEM-ADMIN", properties.getManagement().getAccess().getRole());
        assertTrue(properties.getManagement().getInfo().isPublic());
        assertTrue(properties.getManagement().getLogs().isSystemAdminOnly());
        assertEquals("true", properties.getTenantCache().getEnabled());
        assertEquals("", properties.getTenantCache().getRedis().getPort());
        assertEquals("true", properties.getTenantMemoryCache().getEnabled());
        assertTrue(properties.getXmAutoConfigurationFilter().isDisabled());
    }
}
