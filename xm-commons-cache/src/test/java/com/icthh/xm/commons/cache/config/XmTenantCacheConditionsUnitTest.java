package com.icthh.xm.commons.cache.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;

import com.icthh.xm.commons.cache.TenantCacheManager;
import com.icthh.xm.commons.cache.service.DynamicTenantCacheManager;
import com.icthh.xm.commons.cache.service.XmCacheConfigurer;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

/**
 * Checks which cache beans are created for each combination of
 * {@code application.tenant-memory-cache.enabled} and {@code application.tenant-cache.enabled}.
 */
public class XmTenantCacheConditionsUnitTest {

    private static final String MEMORY = "application.tenant-memory-cache.enabled";
    private static final String TENANT = "application.tenant-cache.enabled";

    @Test
    public void noFlags_onlyStubIsCreated() {
        try (AnnotationConfigApplicationContext ctx = context(Map.of())) {
            assertThat(ctx.getBeansOfType(XmCacheConfigurer.class).keySet(), hasSize(0));
            assertThat(ctx.getBeansOfType(DynamicTenantCacheManager.class).keySet(), hasSize(0));
            assertThat(ctx.getBeansOfType(TenantCacheManager.class).keySet(),
                containsInAnyOrder("tenantAwareCacheManager"));
            assertThat(ctx.getBean("tenantAwareCacheManager").getClass().getName(), containsString("XmTenantConfig"));
        }
    }

    @Test
    public void explicitFalseFlags_onlyStubIsCreated() {
        try (AnnotationConfigApplicationContext ctx = context(Map.of(MEMORY, "false", TENANT, "false"))) {
            assertThat(ctx.getBeansOfType(DynamicTenantCacheManager.class).keySet(), hasSize(0));
            assertThat(ctx.getBean("tenantAwareCacheManager").getClass().getName(), containsString("XmTenantConfig"));
        }
    }

    @Test
    public void memoryCacheOnly_legacyBehaviourUnchanged() {
        try (AnnotationConfigApplicationContext ctx = context(Map.of(MEMORY, "true"))) {
            assertThat(ctx.getBeansOfType(XmCacheConfigurer.class).keySet(), hasSize(1));
            assertThat(ctx.getBeansOfType(DynamicTenantCacheManager.class).keySet(), hasSize(1));
            assertThat(ctx.getBeansOfType(TenantCacheManager.class).keySet(), containsInAnyOrder("lepCacheManager"));
        }
    }

    @Test
    public void tenantCacheOnly_worksWithoutMemoryFlag() {
        try (AnnotationConfigApplicationContext ctx = context(Map.of(TENANT, "true"))) {
            assertThat(ctx.getBeansOfType(XmCacheConfigurer.class).keySet(), hasSize(1));
            assertThat(ctx.getBeansOfType(DynamicTenantCacheManager.class).keySet(), hasSize(1));
            assertThat(ctx.getBeansOfType(TenantCacheManager.class).keySet(),
                containsInAnyOrder("lepCacheManager", "tenantAwareCacheManager"));
            assertThat(ctx.getBean("tenantAwareCacheManager").getClass().getName(),
                containsString("TenantAwareCacheManager"));
        }
    }

    @Test
    public void bothFlags_sameAsBefore() {
        try (AnnotationConfigApplicationContext ctx = context(Map.of(MEMORY, "true", TENANT, "true"))) {
            assertThat(ctx.getBeansOfType(XmCacheConfigurer.class).keySet(), hasSize(1));
            assertThat(ctx.getBeansOfType(DynamicTenantCacheManager.class).keySet(), hasSize(1));
            assertThat(ctx.getBeansOfType(TenantCacheManager.class).keySet(),
                containsInAnyOrder("lepCacheManager", "tenantAwareCacheManager"));
        }
    }

    private AnnotationConfigApplicationContext context(Map<String, String> flags) {
        Map<String, Object> props = new HashMap<>(flags);
        props.put("spring.application.name", "test");

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", props));
        ctx.register(XmTenantConfig.class, XmTenantLepCacheConfig.class, XmTenantCacheConfig.class);
        ctx.registerBean(TenantContextHolder.class, () -> mock(TenantContextHolder.class));
        // @EnableCaching requires a single default CacheManager, like the services do with their @Primary one
        ctx.registerBean("fallbackCacheManager", CacheManager.class,
            () -> new ConcurrentMapCacheManager(), bd -> bd.setPrimary(true));
        ctx.refresh();
        return ctx;
    }
}
