package com.icthh.xm.commons.cache.redis;

import com.icthh.xm.commons.cache.TenantCacheManager;
import com.icthh.xm.commons.cache.config.XmTenantLepCacheConfig;
import com.icthh.xm.commons.cache.service.InitCachesEvent;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class DynamicRedisCacheManagerUnitTest {

    private static final String TENANT = "TEST";

    private DynamicRedisCacheManager manager;

    @Before
    public void setUp() {
        RedisCacheWriter writer = mock(RedisCacheWriter.class);
        manager = new DynamicRedisCacheManager(writer, RedisCacheConfiguration.defaultCacheConfig());
    }

    @Test
    public void registersRedisCaches() {
        XmTenantLepCacheConfig.XmCacheConfiguration cfg = new XmTenantLepCacheConfig.XmCacheConfiguration();
        cfg.setCacheName("rc");
        cfg.setStrategy("REDIS");
        cfg.setExpireAfterWrite(120);

        manager.applyTenantConfig(new InitCachesEvent(this, TENANT, List.of(cfg)));

        Cache cache = manager.getCache(TenantCacheManager.buildKey(TENANT, "rc"));
        assertNotNull(cache);
        assertEquals(1, manager.getCacheNames().size());
    }

    @Test
    public void ignoresCaffeineEntries() {
        XmTenantLepCacheConfig.XmCacheConfiguration cfg = new XmTenantLepCacheConfig.XmCacheConfiguration();
        cfg.setCacheName("cc");
        cfg.setStrategy("CAFFEINE");

        manager.applyTenantConfig(new InitCachesEvent(this, TENANT, List.of(cfg)));

        try {
            manager.getCache(TenantCacheManager.buildKey(TENANT, "cc"));
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // ok
        }
        assertEquals(0, manager.getCacheNames().size());
    }

    @Test(expected = IllegalStateException.class)
    public void unknownCacheNameThrows() {
        manager.getCache(TenantCacheManager.buildKey(TENANT, "missing"));
    }

    @Test
    public void reportsRedisStrategyName() {
        assertEquals("REDIS", manager.getStrategyName());
    }
}
