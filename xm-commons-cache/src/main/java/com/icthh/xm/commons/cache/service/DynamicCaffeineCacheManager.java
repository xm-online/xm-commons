package com.icthh.xm.commons.cache.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.icthh.xm.commons.cache.TenantCacheManager;
import com.icthh.xm.commons.cache.config.XmTenantLepCacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationListener;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.icthh.xm.commons.cache.config.XmTenantLepCacheConfig.CACHE_DEFAULTS;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
@Slf4j
public class DynamicCaffeineCacheManager extends CaffeineCacheManager implements StrategyCacheManager {

    public static final String STRATEGY = "CAFFEINE";

    @Override
    public String getStrategyName() {
        return STRATEGY;
    }

    private final ConcurrentMap<String, Supplier<Caffeine>> cacheCfgMap = new ConcurrentHashMap<>(16);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Ticker ticker;

    @Override
    public Set<String> applyTenantConfig(InitCachesEvent event) {
        final String tenantName = event.getTenantKey().toUpperCase();

        List<Pair<String, XmTenantLepCacheConfig.XmCacheConfiguration>> cacheConfigurationList =
            event.getCacheList().stream()
                .filter(this::isOwnedStrategy)
                .map(cfg -> Pair.of(TenantCacheManager.buildKey(tenantName,cfg.getCacheName()), cfg))
                .toList();

        boolean isLocked = false;
        try {
            isLocked = lock.writeLock().tryLock(5, TimeUnit.SECONDS);
            if (isLocked) {
                final String cachePrefix = TenantCacheManager.buildPrefix(tenantName);

                //remove all CFG for tenant
                Set<String> toRemove = this.cacheCfgMap.keySet().stream()
                    .filter(entry -> entry.startsWith(cachePrefix))
                    .collect(Collectors.toSet());

                //add suppliers for cache builders
                for (Pair<String, XmTenantLepCacheConfig.XmCacheConfiguration> pair: cacheConfigurationList) {
                    this.cacheCfgMap.put(pair.getKey(), () -> buildSpec(pair.getValue()));
                    toRemove.remove(pair.getKey());
                }
                return toRemove;
            }
        } catch (InterruptedException e) {
            log.error("Tenant[" + tenantName + "] " +  e.getMessage(), e);
        } finally {
            if (isLocked) {
                lock.writeLock().unlock();
            }
        }
        return Set.of();
    }

    @Override
    public void cleanTenantConfig(Set<String> cacheKeys) {
        cacheKeys.forEach(cacheCfgMap::remove);
    }

    @Override
    public org.springframework.cache.Cache getCache(String name) {
        if (!cacheCfgMap.keySet().contains(name)) {
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
                Supplier<Caffeine> caffeineSupplier = cacheCfgMap.get(name);
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

    private boolean isOwnedStrategy(XmTenantLepCacheConfig.XmCacheConfiguration cfg) {
        String s = cfg.getStrategy();
        return s == null || s.isBlank() || STRATEGY.equalsIgnoreCase(s);
    }

    private Caffeine buildSpec(XmTenantLepCacheConfig.XmCacheConfiguration cacheConfiguration) {
        Caffeine c = Caffeine.newBuilder().ticker(ticker);

        setMaxSize(c, cacheConfiguration);
        setMaxWeight(c, cacheConfiguration);
        setExpireAfterWrite(c, cacheConfiguration);
        setExpireAfterAccess(c, cacheConfiguration);

        if (cacheConfiguration.isRecordStats()) {
            c.recordStats();
        }

        return c;
    }

    private void setMaxSize(Caffeine cache, XmTenantLepCacheConfig.XmCacheConfiguration cacheConfiguration) {
        Integer maxItems = cacheConfiguration.getMaximumSize();
        if (!CACHE_DEFAULTS.equals(maxItems)) {
            cache.maximumSize(maxItems);
        }
    }

    private void setMaxWeight(Caffeine cache, XmTenantLepCacheConfig.XmCacheConfiguration cacheConfiguration) {
        Integer maxWeight = cacheConfiguration.getMaximumWeight();
        if (!CACHE_DEFAULTS.equals(maxWeight)) {
            cache.maximumWeight(maxWeight);
        }
    }

    private void setExpireAfterWrite(Caffeine cache, XmTenantLepCacheConfig.XmCacheConfiguration cacheConfiguration) {
        Integer ttlSeconds = cacheConfiguration.getExpireAfterWrite();
        if (!CACHE_DEFAULTS.equals(ttlSeconds)) {
            cache.expireAfterWrite(ttlSeconds, TimeUnit.SECONDS);
        }
    }

    private void setExpireAfterAccess(Caffeine cache, XmTenantLepCacheConfig.XmCacheConfiguration cacheConfiguration) {
        Integer ttlSeconds = cacheConfiguration.getExpireAfterAccess();
        if (!CACHE_DEFAULTS.equals(ttlSeconds)) {
            cache.expireAfterAccess(ttlSeconds, TimeUnit.SECONDS);
        }
    }

}
