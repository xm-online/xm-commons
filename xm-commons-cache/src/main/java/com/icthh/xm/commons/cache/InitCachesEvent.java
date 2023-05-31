package com.icthh.xm.commons.cache;

import com.icthh.xm.commons.cache.config.XmCacheConfig;
import groovy.transform.Immutable;
import org.springframework.context.ApplicationEvent;

import java.util.List;

class InitCachesEvent extends ApplicationEvent {

    private String tenantKey;
    private List<XmCacheConfig.XmCacheConfiguration> cacheConfiguration;

    public InitCachesEvent(Object source, String tenantKey, List<XmCacheConfig.XmCacheConfiguration> cacheConfiguration) {
        super(source);
        this.tenantKey = tenantKey;
        this.cacheConfiguration = cacheConfiguration;
    }

    public String getTenantKey() {
        return tenantKey;
    }

    public List<XmCacheConfig.XmCacheConfiguration> getCacheList() {
        return cacheConfiguration;
    }

}
