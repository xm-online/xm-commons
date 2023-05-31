package com.icthh.xm.commons.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.github.benmanes.caffeine.cache.Ticker;
import com.icthh.xm.commons.cache.config.XmCacheConfig;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationListener;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DynamicCaffeineCacheManager extends CaffeineCacheManager implements ApplicationListener<InitCachesEvent> {

    private final ConcurrentMap<String, CaffeineSpec> builderMap = new ConcurrentHashMap<>(16);

    private final Ticker ticker;

    @Override
    public void onApplicationEvent(InitCachesEvent event) {
        final String cachePrefix = event.getTenantKey().toUpperCase();

        List<Pair<String, Caffeine>> collect =
            event.getCacheList().stream()
                .map(cfg -> Pair.of(cfg.getCacheName(), buildSpec(cfg)))
                .collect(Collectors.toList());

/*
        builderMap.putAll();

        final List<String> registeredCaches = getCacheNames()
            .stream()
            .filter(it -> it.startsWith(cachePrefix)).collect(Collectors.toList());

        //there are no caches, and new one should be created
        if (registeredCaches.isEmpty()) {
            event.getCacheList().forEach(it -> createCacheInstance(cachePrefix, it));
            return;
        }

        //there are no caches in incomming configuration
        if (event.getCacheList().isEmpty()) {

        }*/
    }

    @Override
    protected Cache<Object, Object> createNativeCaffeineCache(String name) {
        return super.createNativeCaffeineCache(name);
    }

    private Caffeine buildSpec(XmCacheConfig.XmCacheConfiguration cacheConfiguration) {
        return Caffeine.newBuilder()
            .ticker(ticker);
    }

}
