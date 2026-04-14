package com.icthh.xm.commons.cache.config;

import com.github.benmanes.caffeine.cache.Ticker;
import com.icthh.xm.commons.cache.service.DynamicCaffeineCacheManager;
import com.icthh.xm.commons.cache.service.DynamicTenantCacheManager;
import com.icthh.xm.commons.cache.service.StrategyCacheManager;
import com.icthh.xm.commons.cache.service.TenantAwareCacheManager;
import com.icthh.xm.commons.cache.TenantCacheManager;
import com.icthh.xm.commons.cache.service.XmCacheConfigurer;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnProperty(value = "application.tenant-memory-cache.enabled", havingValue = "true")
public class XmTenantLepCacheConfig {

    public final static Integer CACHE_DEFAULTS = -1;
    public final static String DEFAULT_STRATEGY = "CAFFEINE";

    @Bean
    @ConditionalOnMissingBean(Ticker.class)
    public Ticker ticker() {
        return Ticker.systemTicker();
    }

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
    @ConditionalOnMissingBean(XmCacheConfigurer.class)
    public XmCacheConfigurer xmCacheConfigurer(@Value("${spring.application.name}") String appName,
                                               ApplicationEventPublisher applicationEventPublisher) {
        return new XmCacheConfigurer(appName, applicationEventPublisher);
    }

    /**
     * Dynamic cache to be used in LEP services
     * @param dynamicTenantCacheManager - strategy-routing cache manager
     * @param tenantContextHolder - tenant context
     */
    @Bean
    @Qualifier("lepCacheManager")
    public TenantCacheManager lepCacheManager(DynamicTenantCacheManager dynamicTenantCacheManager,
                                              TenantContextHolder tenantContextHolder) {
        return new TenantAwareCacheManager(dynamicTenantCacheManager, tenantContextHolder);
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class XmCacheConfiguration {
        private String cacheName;
        private String strategy = DEFAULT_STRATEGY;
        private Integer initialCapacity = CACHE_DEFAULTS;
        private Integer maximumSize = CACHE_DEFAULTS;
        private Integer maximumWeight = CACHE_DEFAULTS;
        private Integer expireAfterWrite = CACHE_DEFAULTS;
        private Integer expireAfterAccess = CACHE_DEFAULTS;
        private boolean recordStats;
    }

}
