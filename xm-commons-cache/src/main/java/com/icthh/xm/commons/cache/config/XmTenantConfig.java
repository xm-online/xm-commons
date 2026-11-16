package com.icthh.xm.commons.cache.config;

import com.icthh.xm.commons.cache.TenantCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.List;

/**
 * No-op {@link TenantCacheManager} for services where caching is not configured.
 * Active only when neither {@code application.tenant-cache.enabled} nor the legacy
 * {@code application.tenant-memory-cache.enabled} is {@code true}.
 * <p>
 * TODO(cache-flags-migration): the warning texts below still name the legacy flag; point them to
 * {@code application.tenant-cache.enabled} when the legacy flag is retired.
 */
@Slf4j
@Configuration
@Conditional(NoCacheEnabledCondition.class)
public class XmTenantConfig {

    /**
     * Default TenantCacheManager implementation for spring autowire
     */
    @Bean
    @Qualifier("lepCacheManager")
    public TenantCacheManager tenantAwareCacheManager() {
        return new TenantCacheManager() {

            @Override
            public Cache getCache(String name) {
                log.warn("Property 'application.tenant-memory-cache.enabled' is not configured for getCache({})", name);
                return null;
            }

            @Override
            public Collection<String> getCacheNames() {
                log.warn("Property 'application.tenant-memory-cache.enabled' is not configured for getCacheNames()");
                return List.of();
            }

            @Override
            public void evictCaches() {
                log.warn("Property 'application.tenant-memory-cache.enabled' is not configured for evictCaches");
            }
        };
    }
}
