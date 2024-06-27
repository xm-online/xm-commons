package com.icthh.xm.commons.cache.service;

import com.icthh.xm.commons.cache.config.XmTenantLepCacheConfig;
import lombok.experimental.UtilityClass;

import java.util.function.Consumer;

@UtilityClass
public class CacheUtilityClass {
    public static XmTenantLepCacheConfig.XmCacheConfiguration buildCfg(String name) {
        return buildCfg(c -> {
            c.setCacheName(name);
            c.setMaximumSize(5);
            c.setExpireAfterWrite(5);
            c.setRecordStats(true);
        });
    }

    public static XmTenantLepCacheConfig.XmCacheConfiguration buildCfg(Consumer<XmTenantLepCacheConfig.XmCacheConfiguration> consumer) {
        XmTenantLepCacheConfig.XmCacheConfiguration c = new XmTenantLepCacheConfig.XmCacheConfiguration();
        consumer.accept(c);
        return c;
    }

}
