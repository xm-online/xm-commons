package com.icthh.xm.commons.cache.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.icthh.xm.commons.cache.TenantCacheManager;
import com.icthh.xm.commons.cache.config.XmTenantLepCacheConfig;
import com.icthh.xm.commons.cache.service.builder.CaffeineCacheSpecBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationListener;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

@Slf4j
public class DynamicCaffeineCacheManager extends CaffeineCacheManager implements ApplicationListener<InitCachesEvent> {

    private final ConcurrentMap<String, Supplier<Caffeine<Object, Object>>> cacheCfgMap = new ConcurrentHashMap<>(16);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final CaffeineCacheSpecBuilder specBuilder;

    public DynamicCaffeineCacheManager (Ticker ticker) {
        this.specBuilder = new CaffeineCacheSpecBuilder(ticker);
    }

    @Override
    public void onApplicationEvent(InitCachesEvent event) {
        final String tenantName = event.getTenantKey().toUpperCase();

        List<Pair<String, XmTenantLepCacheConfig.XmCacheConfiguration>> cacheConfigurationList =
            event.getCacheList().stream()
                .map(cfg -> Pair.of(TenantCacheManager.buildKey(tenantName,cfg.getCacheName()), cfg))
                .toList();

        boolean isLocked = false;
        try {
            isLocked = lock.writeLock().tryLock(5, TimeUnit.SECONDS);
            if (isLocked) {
                final String cachePrefix = TenantCacheManager.buildPrefix(tenantName);

                //remove all CFG for tenant
                this.cacheCfgMap.entrySet().removeIf(entry -> entry.getKey().startsWith(cachePrefix));

                //configure future cache instances
                //no cache will be inited, before first call is made
                for (Pair<String, XmTenantLepCacheConfig.XmCacheConfiguration> pair: cacheConfigurationList) {
                    this.cacheCfgMap.put(pair.getKey(), specBuilder.buildSpec(pair.getValue()));
                }
            }
        } catch (InterruptedException e) {
            log.error("Tenant[" + tenantName + "] " +  e.getMessage(), e);
        } finally {
            if (isLocked) {
                lock.writeLock().unlock();
            }
        }

    }

    @Override
    public org.springframework.cache.Cache getCache(String name) {
        if (!cacheCfgMap.containsKey(name)) {
            throw new IllegalStateException("Cache with name " + name + " did not exist in cache mapping");
        }
        return super.getCache(name);
    }

    @Override
    protected Cache<Object, Object> createNativeCaffeineCache(String name) {
        boolean isLocked = false;
        Cache<Object, Object> cache = null;
        try {
            isLocked = lock.readLock().tryLock(5, TimeUnit.SECONDS);
            if (isLocked) {
                Supplier<Caffeine<Object, Object>> caffeineSupplier = cacheCfgMap.get(name);
                Objects.requireNonNull(caffeineSupplier, "Cache configuration [" + name + "] not present in cache map");
                cache = caffeineSupplier.get().build();
            }
        } catch (InterruptedException e) {
            log.error("cache[" + name + "] creation failure" +  e.getMessage(), e);
        } finally {
            if (isLocked) {
                lock.readLock().unlock();
            }
        }
        Objects.requireNonNull(cache, "Cache [" + name + "] is locked");
        return cache;
    }

}
