package com.icthh.xm.commons.cache.service;

import com.icthh.xm.commons.cache.TenantCacheManager;
import com.icthh.xm.commons.cache.config.XmTenantLepCacheConfig.XmCacheConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationListener;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.icthh.xm.commons.cache.TenantCacheManager.buildKey;
import static com.icthh.xm.commons.cache.config.XmTenantLepCacheConfig.DEFAULT_STRATEGY;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * Dispatching {@link CacheManager} that routes per-cache requests to the
 * underlying {@link StrategyCacheManager} matching the strategy declared in
 * {@code cache.yml}. New strategies can be plugged in by registering additional
 * {@link StrategyCacheManager} beans in the Spring context.
 */
@Slf4j
public class DynamicTenantCacheManager implements CacheManager, ApplicationListener<InitCachesEvent> {

    private final Map<String, StrategyCacheManager> strategies;

    /** Maps fully-qualified cache key (tenant@cacheName) → strategy name. */
    private final ConcurrentMap<String, String> cacheStrategyByKey = new ConcurrentHashMap<>();

    public DynamicTenantCacheManager(List<StrategyCacheManager> strategies) {
        this.strategies = strategies.stream().collect(toUnmodifiableMap(
            s -> s.getStrategyName().toUpperCase(),
            identity()
        ));
        log.info("DynamicTenantCacheManager initialized with strategies: {}", this.strategies.keySet());
    }

    @Override
    public void onApplicationEvent(InitCachesEvent event) {
        final String tenantName = event.getTenantKey().toUpperCase();
        final String prefix = TenantCacheManager.buildPrefix(tenantName);

        Map<String, String> next = new HashMap<>();
        for (XmCacheConfiguration cfg : event.getCacheList()) {
            String strategy = resolveStrategy(cfg);
            if (!strategies.containsKey(strategy)) {
                log.warn("Tenant[{}] cache[{}] declares unknown strategy [{}]; available={}",
                    tenantName, cfg.getCacheName(), strategy, strategies.keySet());
                continue;
            }
            next.put(buildKey(tenantName, cfg.getCacheName()), strategy);
        }

        // 1 - update cache strategies (add new caches)
        Map<String, Set<String>> toRemove = new HashMap<>();
        strategies.forEach((key, s) -> {
            Set<String> keyToRemove = s.applyTenantConfig(event);
            toRemove.put(key, keyToRemove);
        });

        // 2 - update routing by strategy
        cacheStrategyByKey.putAll(next);
        cacheStrategyByKey.entrySet().removeIf(
            e -> e.getKey().startsWith(prefix) && !next.containsKey(e.getKey())
        );

        // 3 - clear old caches that no longer exist
        strategies.forEach((key, s) -> s.cleanTenantConfig(toRemove.get(key)));
    }

    @Override
    public Cache getCache(String name) {
        String strategy = cacheStrategyByKey.get(name);
        requireNonNull(strategy, "Cache with name " + name + " did not exist in cache mapping");
        StrategyCacheManager manager = strategies.get(strategy);
        requireNonNull(manager, "No cache manager registered for strategy " + strategy);
        return manager.getCache(name);
    }

    private void requireNonNull(Object obj, String message) {
        if (obj == null) {
            throw new IllegalStateException(message);
        }
    }

    @Override
    public Collection<String> getCacheNames() {
        Set<String> names = new LinkedHashSet<>();
        for (StrategyCacheManager mgr : strategies.values()) {
            names.addAll(mgr.getCacheNames());
        }
        return names;
    }

    private static String resolveStrategy(XmCacheConfiguration cfg) {
        String s = cfg.getStrategy();
        if (s == null || s.isBlank()) {
            return DEFAULT_STRATEGY;
        }
        return s.toUpperCase();
    }
}
