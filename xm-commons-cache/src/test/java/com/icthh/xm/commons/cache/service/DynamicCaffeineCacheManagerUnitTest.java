package com.icthh.xm.commons.cache.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Ticker;
import com.icthh.xm.commons.cache.TenantCacheManager;
import com.icthh.xm.commons.cache.config.XmCacheConfig;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DynamicCaffeineCacheManagerUnitTest {

    DynamicCaffeineCacheManager dynamicCaffeineCacheManager;

    @Test(expected = IllegalStateException.class)
    public void getCacheFailsIfNoCfgProvided() {
        dynamicCaffeineCacheManager = new DynamicCaffeineCacheManager(Ticker.systemTicker());
        org.springframework.cache.Cache cache = dynamicCaffeineCacheManager.getCache(TenantCacheManager.buildKey("TEST", "tcache"));
        assertNotNull(cache);
    }

    @Test
    public void getNewCache() {
        dynamicCaffeineCacheManager = new DynamicCaffeineCacheManager(Ticker.systemTicker());
        XmCacheConfig.XmCacheConfiguration c = CacheUtilityClass.buildCfg("tcache");
        InitCachesEvent e = new InitCachesEvent(this, "test", List.of(c));
        dynamicCaffeineCacheManager.onApplicationEvent(e);
        org.springframework.cache.Cache cache = dynamicCaffeineCacheManager.getCache(TenantCacheManager.buildKey("TEST", "tcache"));
        assertNotNull(cache);
    }

    @Test
    public void getExistingCache() {
        dynamicCaffeineCacheManager = new DynamicCaffeineCacheManager(Ticker.systemTicker());
        XmCacheConfig.XmCacheConfiguration c = CacheUtilityClass.buildCfg("tcache");
        InitCachesEvent e = new InitCachesEvent(this, "test", List.of(c));
        dynamicCaffeineCacheManager.onApplicationEvent(e);
        org.springframework.cache.Cache cache1 = dynamicCaffeineCacheManager.getCache(TenantCacheManager.buildKey("TEST", "tcache"));
        assertNotNull(cache1);
        org.springframework.cache.Cache cache2  = dynamicCaffeineCacheManager.getCache(TenantCacheManager.buildKey("TEST", "tcache"));
        assertEquals(cache1, cache2);
    }

    @Test(expected = NullPointerException.class)
    public void createNativeCaffeineCacheShouldFailIfNoConfigurationPresent() {
        dynamicCaffeineCacheManager = new DynamicCaffeineCacheManager(Ticker.systemTicker());
        dynamicCaffeineCacheManager.createNativeCaffeineCache("test");
    }

    @Test
    public void createCacheObject() {
        dynamicCaffeineCacheManager = new DynamicCaffeineCacheManager(Ticker.systemTicker());

        XmCacheConfig.XmCacheConfiguration c = CacheUtilityClass.buildCfg("tcache");

        InitCachesEvent e = new InitCachesEvent(this, "test", List.of(c));
        dynamicCaffeineCacheManager.onApplicationEvent(e);
        Cache<Object, Object> test = dynamicCaffeineCacheManager.createNativeCaffeineCache(TenantCacheManager.buildKey("TEST", "tcache"));
        assertNotNull(test);
    }


}
