package com.icthh.xm.commons.cache.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;

/**
 * Matches when neither {@code application.tenant-memory-cache.enabled}
 * nor {@code application.tenant-cache.enabled} is {@code true}.
 * <p>
 * TEMPORARY (cache-flags-migration): the inverse of {@link AnyCacheEnabledCondition}, used by the no-op
 * {@link XmTenantConfig} stub so it never overrides the real cache manager when either flag is on.
 * <p>
 * TODO(cache-flags-migration): after the old flag is retired, replace with
 * {@code @ConditionalOnProperty(value = "application.tenant-cache.enabled", havingValue = "false", matchIfMissing = true)}
 * and delete this class.
 */
class NoCacheEnabledCondition extends NoneNestedConditions {

    NoCacheEnabledCondition() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty(value = "application.tenant-memory-cache.enabled", havingValue = "true")
    static class MemoryCacheEnabled {
    }

    @ConditionalOnProperty(value = "application.tenant-cache.enabled", havingValue = "true")
    static class TenantCacheEnabled {
    }
}
