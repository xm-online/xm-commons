package com.icthh.xm.commons.cache.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;

import com.icthh.xm.commons.cache.TenantCacheManager;
import com.icthh.xm.commons.cache.service.DynamicTenantCacheManager;
import com.icthh.xm.commons.cache.service.XmCacheConfigurer;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

/**
 * Checks which cache beans are created for each combination of
 * {@code application.tenant-memory-cache.enabled} and {@code application.tenant-cache.enabled}.
 */
public class XmTenantCacheConditionsUnitTest {

    private static final String MEMORY = "application.tenant-memory-cache.enabled";
    private static final String TENANT = "application.tenant-cache.enabled";

    private static final String FALLBACK = "FallbackXmCacheConfiguration";

    @Test
    public void noFlags_onlyFallbackIsCreated() {
        try (AnnotationConfigApplicationContext ctx = context(Map.of())) {
            assertThat(ctx.getBeansOfType(XmCacheConfigurer.class).keySet(), hasSize(0));
            assertThat(ctx.getBeansOfType(DynamicTenantCacheManager.class).keySet(), hasSize(0));
            assertThat(className(ctx, "tenantCacheManager"), containsString(FALLBACK));
            assertThat(className(ctx, "lepCacheManager"), containsString(FALLBACK));
        }
    }

    @Test
    public void explicitFalseFlags_onlyFallbackIsCreated() {
        try (AnnotationConfigApplicationContext ctx = context(Map.of(MEMORY, "false", TENANT, "false"))) {
            assertThat(ctx.getBeansOfType(DynamicTenantCacheManager.class).keySet(), hasSize(0));
            assertThat(className(ctx, "tenantCacheManager"), containsString(FALLBACK));
            assertThat(className(ctx, "lepCacheManager"), containsString(FALLBACK));
        }
    }

    @Test
    public void memoryCacheOnly_legacyBehaviourUnchanged() {
        try (AnnotationConfigApplicationContext ctx = context(Map.of(MEMORY, "true"))) {
            assertThat(ctx.getBeansOfType(XmCacheConfigurer.class).keySet(), hasSize(1));
            assertThat(ctx.getBeansOfType(DynamicTenantCacheManager.class).keySet(), hasSize(1));
            assertThat(className(ctx, "lepCacheManager"), not(containsString(FALLBACK)));
            assertThat(className(ctx, "tenantCacheManager"), containsString(FALLBACK));
        }
    }

    @Test
    public void tenantCacheOnly_worksWithoutMemoryFlag() {
        try (AnnotationConfigApplicationContext ctx = context(Map.of(TENANT, "true"))) {
            assertThat(ctx.getBeansOfType(XmCacheConfigurer.class).keySet(), hasSize(1));
            assertThat(ctx.getBeansOfType(DynamicTenantCacheManager.class).keySet(), hasSize(1));
            assertThat(className(ctx, "tenantCacheManager"), not(containsString(FALLBACK)));
            assertThat(className(ctx, "lepCacheManager"), not(containsString(FALLBACK)));
        }
    }

    @Test
    public void bothFlags_sameAsBefore() {
        try (AnnotationConfigApplicationContext ctx = context(Map.of(MEMORY, "true", TENANT, "true"))) {
            assertThat(ctx.getBeansOfType(XmCacheConfigurer.class).keySet(), hasSize(1));
            assertThat(ctx.getBeansOfType(DynamicTenantCacheManager.class).keySet(), hasSize(1));
            assertThat(className(ctx, "tenantCacheManager"), not(containsString(FALLBACK)));
            assertThat(className(ctx, "lepCacheManager"), not(containsString(FALLBACK)));
        }
    }

    private static String className(AnnotationConfigApplicationContext ctx, String beanName) {
        return ctx.getBean(beanName, TenantCacheManager.class).getClass().getName();
    }

    private AnnotationConfigApplicationContext context(Map<String, String> flags) {
        Map<String, Object> props = new HashMap<>(flags);
        props.put("spring.application.name", "test");

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", props));
        // same order as component scan: classes are loaded alphabetically
        ctx.register(FallbackXmCacheConfiguration.class, XmTenantCacheConfig.class, XmTenantLepCacheConfig.class);
        ctx.registerBean(TenantContextHolder.class, () -> mock(TenantContextHolder.class));
        ctx.registerBean(MeterRegistry.class, SimpleMeterRegistry::new);
        ctx.refresh();
        return ctx;
    }
}
