package com.icthh.xm.commons.cache.service;

import com.icthh.xm.commons.cache.config.XmCacheConfig;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CacheUtilityClass {
    public static XmCacheConfig.XmCacheConfiguration buildCfg(String name) {
        XmCacheConfig.XmCacheConfiguration c = new XmCacheConfig.XmCacheConfiguration();
        c.setCacheName(name);
        c.setMaximumSize(5);
        c.setExpireAfterWrite(5);
        c.setRecordStats(true);
        return c;
    }
}
