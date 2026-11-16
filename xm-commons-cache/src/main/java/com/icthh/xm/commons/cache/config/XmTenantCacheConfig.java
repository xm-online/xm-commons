package com.icthh.xm.commons.cache.config;

import com.icthh.xm.commons.cache.TenantCacheManager;
import com.icthh.xm.commons.cache.service.DynamicTenantCacheManager;
import com.icthh.xm.commons.cache.service.TenantAwareCacheManager;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring caching and exposes the {@code tenantCacheManager} bean.
 * <p>
 * Infrastructure beans (ticker, strategy cache managers, {@code XmCacheConfigurer}) come from
 * {@link XmTenantLepCacheConfig}, which is also activated by {@code application.tenant-cache.enabled=true},
 * so {@code application.tenant-memory-cache.enabled} is not required together with it.
 * <p>
 * MIGRATION (cache-flags-migration): services that still set both flags keep working unchanged. They can drop
 * {@code application.tenant-memory-cache.enabled} and leave only {@code application.tenant-cache.enabled}.
 */
@Configuration
@ConditionalOnProperty(value = "application.tenant-cache.enabled", havingValue = "true")
@EnableCaching
public class XmTenantCacheConfig {

    @Bean
    @Qualifier("tenantCacheManager")
    public TenantCacheManager tenantAwareCacheManager(DynamicTenantCacheManager dynamicTenantCacheManager,
                                                      TenantContextHolder tenantContextHolder) {
        return new TenantAwareCacheManager(dynamicTenantCacheManager, tenantContextHolder);
    }

}
