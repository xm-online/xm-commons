package com.icthh.xm.commons.cache;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
    public Cache getCache(final String name) {
        String currentTenant = tenantContextHolder.getTenantKey();
        if (isTenantInvalid(currentTenant)) {
            return null;
        }
        return delegate.getCache(TenantCacheManager.buildKey(currentTenant, name));
    }

    @Override
    public Collection<String> getCacheNames() {
        String currentTenant = tenantContextHolder.getTenantKey();

        if (isTenantInvalid(currentTenant)) {
            return List.of();
        }

        currentTenant = currentTenant.toUpperCase();

        return getCacheNames(currentTenant);
    }

    @Override
    public void evictCaches() {
        String currentTenant = tenantContextHolder.getTenantKey();
        getCacheNames(currentTenant).forEach(cacheName -> delegate.getCache(TenantCacheManager.buildKey(currentTenant, cacheName)).clear());
    }

    private static boolean isTenantInvalid(final String tenant) {
        return tenant == null || tenant.contains(TENANT_CACHE_DELIMITER);
    }

    private Collection<String> getCacheNames(final String tenant) {
        final String tenantWithDelimiter = tenant + TENANT_CACHE_DELIMITER;
        return delegate.getCacheNames().stream()
            .filter(cacheName -> cacheName.startsWith(tenantWithDelimiter))
            .map(cacheName -> cacheName.substring(tenantWithDelimiter.length()))
            .collect(Collectors.toList());
    }

}
