package com.icthh.xm.commons.cache.config;

import com.github.benmanes.caffeine.cache.Ticker;
import com.icthh.xm.commons.cache.TenantCacheManager;
import com.icthh.xm.commons.cache.service.DynamicCaffeineCacheManager;
import com.icthh.xm.commons.cache.service.TenantAwareCacheManager;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.List;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "application.tenant-memory-cache.enabled", havingValue = "false", matchIfMissing = true)
public class XmTenantConfig {

    @Bean
    @ConditionalOnMissingBean(Ticker.class)
    public Ticker ticker() {
        return Ticker.systemTicker();
    }

    @Bean
    public DynamicCaffeineCacheManager dynamicCaffeineCacheManager(Ticker ticker) {
        return new DynamicCaffeineCacheManager(ticker);
    }

    /**
     * Default TenantCacheManager implementation for spring autowire
     */
    @Bean
    @Qualifier("lepCacheManager")
    public TenantCacheManager tenantAwareCacheManager(DynamicCaffeineCacheManager caffeineCacheManager,
                                                      TenantContextHolder tenantContextHolder) {
        return new TenantAwareCacheManager(caffeineCacheManager, tenantContextHolder) {

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
