package com.icthh.xm.commons.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.icthh.xm.commons.cache.config.XmCacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class TenantAwareCacheManagerService implements CacheManager, ApplicationListener<InitCachesEvent>  {

    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(16);
    private volatile Set<String> cacheNames = Collections.emptySet();

    private final Ticker ticker;

    @Override
    @Nullable
    public Cache getCache(String name) {
        // Quick check for existing cache...
        Cache cache = this.cacheMap.get(name);
        if (cache != null) {
            return cache;
        }

        // The provider may support on-demand cache creation...
        /*Cache missingCache = getMissingCache(name);
        if (missingCache != null) {
            // Fully synchronize now for missing cache registration
            synchronized (this.cacheMap) {
                cache = this.cacheMap.get(name);
                if (cache == null) {
                    cache = decorateCache(missingCache);
                    this.cacheMap.put(name, cache);
                    updateCacheNames(name);
                }
            }
        }
        return cache;*/
        return null;
    }

/*
    private void initCache(String tenant, XmCacheConfig.XmCacheConfiguration cfg) {
        String cacheName = cacheName(tenant, cfg.getCacheName());
        // Quick check for existing cache...
        Cache cache = this.cacheMap.get(cacheName);
        if (cache != null) {
            return;
        }
        synchronized (this.cacheMap) {
            cache = this.cacheMap.get(cacheName);
            if (cache == null) {
                cache = createCacheInstance(cacheName, cfg);
                this.cacheMap.put(cacheName, cache);
                updateCacheNames(name);
            }
        }
    }

    private static CaffeineCache buildCaffeineCache(String name, CacheSpecsConfiguration.CacheSpec cacheSpec, Ticker ticker) {
        log.info("Cache {} specified timeout of {} min, max of {}",name,cacheSpec.getTimeout(),cacheSpec.getMax());
        final Caffeine<Object, Object> caffeineBuilder
            = Caffeine.newBuilder()
            .expireAfterWrite(cacheSpec.getTimeout(), TimeUnit.MINUTES)
            .maximumSize(cacheSpec.getMax())
            .ticker(ticker)
            .recordStats();
        return new CaffeineCache(name, caffeineBuilder.build());
    }
*/

    @Override
    public void onApplicationEvent(InitCachesEvent event) {
        final String cachePrefix = cachePrefix(event.getTenantKey());

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

        }

        //there are caches, and merge should be applied
        //getMissingCache()

    }

    private Cache createCacheInstance(String cacheName, XmCacheConfig.XmCacheConfiguration it) {
        //return new CaffeineCache(cacheName, createNativeCaffeineCache(it));
        return null;
    }

    protected String cachePrefix(String tenantKey) {
        return tenantKey + "-";
    }

    protected String cacheName(String tenantKey, String cacheKey) {
        return cachePrefix(tenantKey) + cacheKey;
    }


    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(this.cacheMap.keySet());
    }
}
