package com.icthh.xm.commons.cache.config;

import com.icthh.xm.commons.cache.TenantCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.Cache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
@Configuration
public class FallbackXmCacheConfiguration {

    @Bean(name = "tenantCacheManager")
    @ConditionalOnMissingBean(name = "tenantCacheManager")
    public TenantCacheManager tenantCacheManager() {
        log.warn("No tenantCacheManager bean found. Set application.tenant-cache.enabled=true to enable caching.");
        return new TenantCacheManager() {
            @Override
            public Cache getCache(String name) {
                log.warn("Property 'application.tenant-cache.enabled' is not configured for getCache({})", name);
                return null;
            }

            @Override
            public Collection<String> getCacheNames() {
                log.warn("Property 'application.tenant-cache.enabled' is not configured for getCacheNames()");
                return Collections.emptyList();
            }

            @Override
            public void evictCaches() {
                log.warn("Property 'application.tenant-cache.enabled' is not configured for evictCaches");
            }
        };
    }

    @Bean(name = "lepCacheManager")
    @ConditionalOnMissingBean(name = "lepCacheManager")
    public TenantCacheManager lepCacheManager() {
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
