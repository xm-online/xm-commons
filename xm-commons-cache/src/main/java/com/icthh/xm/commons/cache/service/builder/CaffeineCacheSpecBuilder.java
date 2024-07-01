package com.icthh.xm.commons.cache.service.builder;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.icthh.xm.commons.cache.config.XmTenantLepCacheConfig;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.icthh.xm.commons.cache.config.XmTenantLepCacheConfig.CACHE_DEFAULTS;

@RequiredArgsConstructor
public class CaffeineCacheSpecBuilder {

    private final Ticker ticker;

    public Supplier<Caffeine<Object, Object>> buildSpec(XmTenantLepCacheConfig.XmCacheConfiguration cacheConfiguration) {
        return () -> {
            Caffeine<Object, Object> c = Caffeine.newBuilder().ticker(ticker);

            setMaxSize(c, cacheConfiguration);
            setMaxWeight(c, cacheConfiguration);
            setExpireAfterWrite(c, cacheConfiguration);
            setExpireAfterAccess(c, cacheConfiguration);

            if (cacheConfiguration.isRecordStats()) {
                c.recordStats();
            }
            return c;
        };
    }

    private void setMaxSize(Caffeine<Object, Object> cache, XmTenantLepCacheConfig.XmCacheConfiguration cacheConfiguration) {
        Integer maxItems = cacheConfiguration.getMaximumSize();
        if (!CACHE_DEFAULTS.equals(maxItems)) {
            cache.maximumSize(maxItems);
        }
    }

    private void setMaxWeight(Caffeine<Object, Object> cache, XmTenantLepCacheConfig.XmCacheConfiguration cacheConfiguration) {
        Integer maxWeight = cacheConfiguration.getMaximumWeight();
        if (!CACHE_DEFAULTS.equals(maxWeight)) {
            cache.maximumWeight(maxWeight);
        }
    }

    private void setExpireAfterWrite(Caffeine<Object, Object> cache, XmTenantLepCacheConfig.XmCacheConfiguration cacheConfiguration) {
        Integer ttlSeconds = cacheConfiguration.getExpireAfterWrite();
        if (!CACHE_DEFAULTS.equals(ttlSeconds)) {
            cache.expireAfterWrite(ttlSeconds, TimeUnit.SECONDS);
        }
    }

    private void setExpireAfterAccess(Caffeine<Object, Object> cache, XmTenantLepCacheConfig.XmCacheConfiguration cacheConfiguration) {
        Integer ttlSeconds = cacheConfiguration.getExpireAfterAccess();
        if (!CACHE_DEFAULTS.equals(ttlSeconds)) {
            cache.expireAfterAccess(ttlSeconds, TimeUnit.SECONDS);
        }
    }
}
