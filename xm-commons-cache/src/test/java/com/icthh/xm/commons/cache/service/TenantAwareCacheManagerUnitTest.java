package com.icthh.xm.commons.cache.service;

import com.github.benmanes.caffeine.cache.Ticker;
import com.icthh.xm.commons.cache.config.XmTenantLepCacheConfig;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.cache.Cache;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.*;

@Slf4j
public class TenantAwareCacheManagerUnitTest {

    private static final String TENANT = "TEST";
    TenantAwareCacheManager cacheManager;

    TenantContextHolder cth = new DefaultTenantContextHolder();

    @Test
    public void getExistingCache() {
        DynamicCaffeineCacheManager c = new DynamicCaffeineCacheManager(Ticker.systemTicker());
        XmTenantLepCacheConfig.XmCacheConfiguration tcache = CacheUtilityClass.buildCfg("tcache");
        InitCachesEvent e = new InitCachesEvent(this, TENANT, List.of(tcache));
        c.onApplicationEvent(e);
        cth.getPrivilegedContext().destroyCurrentContext();
        TenantContextUtils.setTenant(cth, TENANT);
        cacheManager = new TenantAwareCacheManager(c, cth);
        Cache cache = cacheManager.getCache("tcache");
        assertNotNull(cache);
    }

    @Test
    public void getNullForTenantWithForbiddenFormat() {
        DynamicCaffeineCacheManager c = new DynamicCaffeineCacheManager(Ticker.systemTicker());
        cth.getPrivilegedContext().destroyCurrentContext();
        TenantContextUtils.setTenant(cth, "TEN@NT");
        cacheManager = new TenantAwareCacheManager(c, cth);
        Cache cache = cacheManager.getCache("notExistingCache");
        assertNull(cache);
    }

    @Test
    public void getCacheNames() {
        DynamicCaffeineCacheManager c = new DynamicCaffeineCacheManager(Ticker.systemTicker());
        XmTenantLepCacheConfig.XmCacheConfiguration tcache = CacheUtilityClass.buildCfg("tcache");
        InitCachesEvent e = new InitCachesEvent(this, TENANT, List.of(tcache));
        c.onApplicationEvent(e);
        cth.getPrivilegedContext().destroyCurrentContext();
        TenantContextUtils.setTenant(cth, TENANT);
        cacheManager = new TenantAwareCacheManager(c, cth);

        Collection<String> cacheNames = cacheManager.getCacheNames();
        assertEquals(0, cacheNames.size());

        Cache cache = cacheManager.getCache("tcache");
        cacheNames = cacheManager.getCacheNames();
        assertNotNull(cache);
        assertEquals(1, cacheNames.size());
    }

    @Test
    public void evictCaches() {
        DynamicCaffeineCacheManager c = new DynamicCaffeineCacheManager(Ticker.systemTicker());
        XmTenantLepCacheConfig.XmCacheConfiguration tcache = CacheUtilityClass.buildCfg("tcache");
        InitCachesEvent e = new InitCachesEvent(this, TENANT, List.of(tcache));
        c.onApplicationEvent(e);
        cth.getPrivilegedContext().destroyCurrentContext();
        TenantContextUtils.setTenant(cth, TENANT);
        cacheManager = new TenantAwareCacheManager(c, cth);

        Cache cache = cacheManager.getCache("tcache");
        cache.put("key", "value");

        cache = cacheManager.getCache("tcache");
        String value = cache.get("key", String.class);
        assertEquals("value", value);

        cacheManager.evictCaches();
        value = cache.get("key", String.class);
        assertNull(value);
    }

    @Test
    public void testReadThroughCacheUsageOptions() throws InterruptedException {

        AtomicInteger ctr = new AtomicInteger();

        Supplier<Integer> getValue = ctr::incrementAndGet;

        DynamicCaffeineCacheManager c = new DynamicCaffeineCacheManager(Ticker.systemTicker());
        XmTenantLepCacheConfig.XmCacheConfiguration tcache = CacheUtilityClass.buildCfg("tcache");
        InitCachesEvent e = new InitCachesEvent(this, TENANT, List.of(tcache));
        c.onApplicationEvent(e);
        cth.getPrivilegedContext().destroyCurrentContext();
        TenantContextUtils.setTenant(cth, TENANT);
        cacheManager = new TenantAwareCacheManager(c, cth);

        Cache cache = cacheManager.getCache("tcache");

        Integer value = cache.get("value", () -> getValue.get());
        assertEquals(1, value.intValue());

        value = cache.get("value", () -> getValue.get());//get from cache (way1)
        assertEquals(1, value.intValue());
        value = cache.get("value", Integer.class);//get from cache (way2)
        assertEquals(1, value.intValue());

        Thread.currentThread().sleep(6000);//sleep for expiry time
        value = cache.get("value", Integer.class);
        assertNull(value);

        value = cache.get("value", () -> getValue.get());//get from cache
        assertEquals(2, value.intValue()); //get some changed value from service

    }

}
