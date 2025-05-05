package com.icthh.xm.commons.cache.config;

import com.github.benmanes.caffeine.cache.Ticker;
import com.icthh.xm.commons.cache.TenantCacheManager;
import com.icthh.xm.commons.cache.service.DynamicCaffeineCacheManager;
import com.icthh.xm.commons.cache.service.TenantAwareCacheManager;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "application.tenant-cache.enabled", havingValue = "true")
@EnableCaching
public class XmTenantCacheConfig {

    @Bean
    @ConditionalOnMissingBean(Ticker.class)
    public Ticker ticker() {
        return Ticker.systemTicker();
    }

    /**
     * Specialized cache to be used in Java code
     */
    @Bean
    @Qualifier("tenantCacheManager")
    public TenantCacheManager tenantAwareCacheManager(Ticker ticker, TenantContextHolder tenantContextHolder) {
        DynamicCaffeineCacheManager caffeineCacheManager = new DynamicCaffeineCacheManager(ticker);
        return new TenantAwareCacheManager(caffeineCacheManager, tenantContextHolder);
    }

}
