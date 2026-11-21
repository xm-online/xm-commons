package com.icthh.xm.commons.cache.config;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Matches when at least one of {@code application.tenant-memory-cache.enabled}
 * or {@code application.tenant-cache.enabled} is {@code true}.
 * <p>
 * TEMPORARY (cache-flags-migration): this condition exists only to keep services that still set
 * {@code application.tenant-memory-cache.enabled} working while they move to the single
 * {@code application.tenant-cache.enabled} flag. Once no service uses the old flag, the
 * {@code MemoryCacheEnabled} branch and, with it, the whole condition can be replaced by
 * {@code @ConditionalOnProperty("application.tenant-cache.enabled")}.
 * <p>
 * TODO(cache-flags-migration): remove together with the legacy flag.
 */
class AnyCacheEnabledCondition extends AnyNestedCondition {

    AnyCacheEnabledCondition() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty(value = "application.tenant-memory-cache.enabled", havingValue = "true")
    static class MemoryCacheEnabled {
    }

    @ConditionalOnProperty(value = "application.tenant-cache.enabled", havingValue = "true")
    static class TenantCacheEnabled {
    }
}
