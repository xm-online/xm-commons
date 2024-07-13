package com.icthh.xm.commons.cache.service;

import com.icthh.xm.commons.cache.TenantCacheManager;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class TenantAwareCacheManager implements TenantCacheManager {

    private final CacheManager delegate;

    private final TenantContextHolder tenantContextHolder;

    /**
     * Constructor.
     *
     * @param delegate
     *            the {@link CacheManager} to delegate to.
     * @param tenantContextHolder
     *            context to retrieve the current tenant
     */
    public TenantAwareCacheManager(final CacheManager delegate, final TenantContextHolder tenantContextHolder) {
        this.tenantContextHolder = tenantContextHolder;
        this.delegate = delegate;
    }

    @Override
    @Nullable
    public Cache getCache(final String name) {
        String currentTenant = tenantContextHolder.getTenantKey();
        if (isTenantInvalid(currentTenant)) {
            log.warn("undefined tenant {} trying to access cache delegate", currentTenant);
            return null;
        }
        return delegate.getCache(TenantCacheManager.buildKey(currentTenant, name));
    }

    @Override
    public Collection<String> getCacheNames() {
        String currentTenant = tenantContextHolder.getTenantKey();

        if (isTenantInvalid(currentTenant)) {
            log.warn("undefined tenant {} trying to access cache delegate. Return []", currentTenant);
            return List.of();
        }

        return getCacheNames(currentTenant);
    }

    @Override
    public void evictCaches() {
        String currentTenant = tenantContextHolder.getTenantKey();
        List<Cache> caches = getCacheNames(currentTenant)
            .stream()
            .map(cacheName -> delegate.getCache(TenantCacheManager.buildKey(currentTenant, cacheName)))
            .filter(Objects::nonNull)
            .toList();
        for (Cache cache: caches) {
            log.info("Cleaning cache {}", cache.getName());
            cache.clear();
        }
    }

    private static boolean isTenantInvalid(final String tenant) {
        return tenant == null || tenant.contains(TENANT_CACHE_DELIMITER);
    }

    private Collection<String> getCacheNames(final String tenant) {
        final String tenantWithDelimiter = TenantCacheManager.buildPrefix(tenant);
        return delegate.getCacheNames().stream()
            .filter(cacheName -> cacheName.startsWith(tenantWithDelimiter))
            .map(cacheName -> cacheName.substring(tenantWithDelimiter.length()))
            .collect(Collectors.toList());
    }

}
