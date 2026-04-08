package com.icthh.xm.commons.cache.service;

import com.icthh.xm.commons.cache.config.XmTenantLepCacheConfig.XmCacheConfiguration;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Set;

/**
 * Marker interface for cache managers that implement a particular caching strategy
 * (e.g. CAFFEINE, REDIS). Implementations are auto-discovered by
 * {@link DynamicTenantCacheManager} which routes calls based on the strategy
 * declared in {@code cache.yml} per cache entry.
 */
public interface StrategyCacheManager extends CacheManager {

    /**
     * @return strategy identifier matching the value used in {@code cache.yml}.
     */
    String getStrategyName();

    /**
     * @return cacheKey to remove
     */
    Set<String> applyTenantConfig(InitCachesEvent event);

    void cleanTenantConfig(Set<String> cacheKeys);

}
