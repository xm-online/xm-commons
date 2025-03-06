package com.icthh.xm.commons.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;

public interface TenantCacheManager extends CacheManager {

    String TENANT_CACHE_DELIMITER = "@";

    void evictCaches();

    static String buildKey(final String tenant, final String cacheName) {
        return tenant + TENANT_CACHE_DELIMITER + cacheName;
    }

    static String buildPrefix(final String tenant) {
        return tenant + TENANT_CACHE_DELIMITER;
    }

}
