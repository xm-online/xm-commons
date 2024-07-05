package com.icthh.xm.commons.cache.service;

import com.icthh.xm.commons.cache.config.XmTenantLepCacheConfig;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

class InitCachesEvent extends ApplicationEvent {

    @Getter
    private final String tenantKey;
    private final List<XmTenantLepCacheConfig.XmCacheConfiguration> cacheConfiguration;

    public InitCachesEvent(Object source, String tenantKey, List<XmTenantLepCacheConfig.XmCacheConfiguration> cacheConfiguration) {
        super(source);
        this.tenantKey = tenantKey;
        this.cacheConfiguration = cacheConfiguration;
    }

    public List<XmTenantLepCacheConfig.XmCacheConfiguration> getCacheList() {
        return cacheConfiguration;
    }

}
