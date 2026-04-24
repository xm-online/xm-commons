package com.icthh.xm.commons.cache.config;

import com.github.benmanes.caffeine.cache.Ticker;
import com.icthh.xm.commons.cache.TenantCacheManager;
import com.icthh.xm.commons.cache.service.DynamicCaffeineCacheManager;
import com.icthh.xm.commons.cache.service.DynamicTenantCacheManager;
import com.icthh.xm.commons.cache.service.StrategyCacheManager;
import com.icthh.xm.commons.cache.service.TenantAwareCacheManager;
import com.icthh.xm.commons.tenant.TenantContextHolder;

import java.util.List;
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
    @ConditionalOnMissingBean(DynamicCaffeineCacheManager.class)
    public DynamicCaffeineCacheManager dynamicCaffeineCacheManager(Ticker ticker) {
        return new DynamicCaffeineCacheManager(ticker);
    }

    @Bean
    @ConditionalOnMissingBean(DynamicTenantCacheManager.class)
    public DynamicTenantCacheManager dynamicTenantCacheManager(List<StrategyCacheManager> strategies) {
        return new DynamicTenantCacheManager(strategies);
    }

    @Bean
    @Qualifier("tenantCacheManager")
    public TenantCacheManager tenantAwareCacheManager(DynamicTenantCacheManager dynamicTenantCacheManager,
                                                      TenantContextHolder tenantContextHolder) {
        return new TenantAwareCacheManager(dynamicTenantCacheManager, tenantContextHolder);
    }

}
