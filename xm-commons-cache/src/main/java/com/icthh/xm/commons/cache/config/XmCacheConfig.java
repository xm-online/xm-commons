package com.icthh.xm.commons.cache.config;

import com.github.benmanes.caffeine.cache.Ticker;
import com.icthh.xm.commons.cache.TenantAwareCacheManager;
import com.icthh.xm.commons.cache.TenantCacheManager;
import com.icthh.xm.commons.cache.XmCacheConfigurer;
import com.icthh.xm.commons.cache.TenantAwareCacheManagerService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "application.memory-cache.enabled", havingValue = "true")
public class XmCacheConfig {

    private final static Integer UNLIMITED_STORAGE = -1;

    @Bean
    public Ticker ticker() {
        return Ticker.systemTicker();
    }

    @Bean
    public XmCacheConfigurer xmCacheConfigurer(@Value("${spring.application.name}") String appName,
                                               ApplicationEventPublisher applicationEventPublisher) {
        return new XmCacheConfigurer(appName, applicationEventPublisher);
    }

    @Bean
    public TenantCacheManager tenantAwareCacheManager(Ticker ticker, TenantContextHolder tenantContextHolder) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();

        //caffeineCacheManager.setCaffeineSpec();
        return new TenantAwareCacheManager(caffeineCacheManager, tenantContextHolder);
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class XmCacheConfiguration {
        private String cacheName;
        private Integer ttl = UNLIMITED_STORAGE;
        private Integer maxItems = UNLIMITED_STORAGE;
        private boolean allowNullValue = false;
    }

}
