package com.icthh.xm.commons.cache.service;

import com.icthh.xm.commons.cache.config.XmTenantLepCacheConfig;
import org.springframework.context.ApplicationEvent;

import java.util.List;

class InitCachesEvent extends ApplicationEvent {

    private String tenantKey;
    private List<XmTenantLepCacheConfig.XmCacheConfiguration> cacheConfiguration;

    public InitCachesEvent(Object source, String tenantKey, List<XmTenantLepCacheConfig.XmCacheConfiguration> cacheConfiguration) {
        super(source);
        this.tenantKey = tenantKey;
        this.cacheConfiguration = cacheConfiguration;
    }

    public String getTenantKey() {
        return tenantKey;
    }

    public List<XmTenantLepCacheConfig.XmCacheConfiguration> getCacheList() {
        return cacheConfiguration;
    }

}
