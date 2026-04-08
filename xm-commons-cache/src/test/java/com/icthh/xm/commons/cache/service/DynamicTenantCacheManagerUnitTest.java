package com.icthh.xm.commons.cache.service;

import com.github.benmanes.caffeine.cache.Ticker;
import com.icthh.xm.commons.cache.TenantCacheManager;
import com.icthh.xm.commons.cache.config.XmTenantLepCacheConfig;
import org.junit.Test;
import org.springframework.cache.Cache;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class DynamicTenantCacheManagerUnitTest {

    private static final String TENANT = "TEST";

    @Test
    public void routesToCaffeineByDefault() {
        DynamicCaffeineCacheManager caffeine = new DynamicCaffeineCacheManager(Ticker.systemTicker());
        DynamicTenantCacheManager dispatcher = new DynamicTenantCacheManager(List.of(caffeine));

        XmTenantLepCacheConfig.XmCacheConfiguration cfg = CacheUtilityClass.buildCfg("c1");
        // strategy left at default → CAFFEINE

        InitCachesEvent event = new InitCachesEvent(this, TENANT, List.of(cfg));
        caffeine.applyTenantConfig(event);
        dispatcher.onApplicationEvent(event);

        Cache cache = dispatcher.getCache(TenantCacheManager.buildKey(TENANT, "c1"));
        assertNotNull(cache);
    }

    @Test
    public void routesToExplicitCaffeineStrategy() {
        DynamicCaffeineCacheManager caffeine = new DynamicCaffeineCacheManager(Ticker.systemTicker());
        DynamicTenantCacheManager dispatcher = new DynamicTenantCacheManager(List.of(caffeine));

        XmTenantLepCacheConfig.XmCacheConfiguration cfg = CacheUtilityClass.buildCfg("c1");
        cfg.setStrategy("CAFFEINE");

        InitCachesEvent event = new InitCachesEvent(this, TENANT, List.of(cfg));
        caffeine.applyTenantConfig(event);
        dispatcher.onApplicationEvent(event);

        assertNotNull(dispatcher.getCache(TenantCacheManager.buildKey(TENANT, "c1")));
    }

    @Test
    public void unknownCacheNameThrows() {
        DynamicCaffeineCacheManager caffeine = new DynamicCaffeineCacheManager(Ticker.systemTicker());
        DynamicTenantCacheManager dispatcher = new DynamicTenantCacheManager(List.of(caffeine));
        try {
            dispatcher.getCache(TenantCacheManager.buildKey(TENANT, "missing"));
            fail("Expected IllegalStateException");
        } catch (NullPointerException expected) {
            // ok
        }
    }

    @Test
    public void unknownStrategyIsSkipped() {
        DynamicCaffeineCacheManager caffeine = new DynamicCaffeineCacheManager(Ticker.systemTicker());
        DynamicTenantCacheManager dispatcher = new DynamicTenantCacheManager(List.of(caffeine));

        XmTenantLepCacheConfig.XmCacheConfiguration cfg = CacheUtilityClass.buildCfg("c1");
        cfg.setStrategy("REDIS"); // not registered

        InitCachesEvent event = new InitCachesEvent(this, TENANT, List.of(cfg));
        caffeine.applyTenantConfig(event);
        dispatcher.onApplicationEvent(event);

        try {
            dispatcher.getCache(TenantCacheManager.buildKey(TENANT, "c1"));
            fail("Expected IllegalStateException for unmapped strategy");
        } catch (NullPointerException expected) {
            // ok
        }
        assertEquals(0, dispatcher.getCacheNames().size());
    }
}
