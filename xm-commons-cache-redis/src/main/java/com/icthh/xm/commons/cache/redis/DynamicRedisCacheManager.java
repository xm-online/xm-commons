package com.icthh.xm.commons.cache.redis;

import com.icthh.xm.commons.cache.TenantCacheManager;
import com.icthh.xm.commons.cache.config.XmTenantLepCacheConfig.XmCacheConfiguration;
import com.icthh.xm.commons.cache.service.InitCachesEvent;
import com.icthh.xm.commons.cache.service.StrategyCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static com.icthh.xm.commons.cache.config.XmTenantLepCacheConfig.CACHE_DEFAULTS;
import static java.util.stream.Collectors.toSet;

/**
 * {@link StrategyCacheManager} that delegates to Spring Data Redis. Picks up
 * {@code cache.yml} entries whose {@code strategy} equals {@code REDIS} and
 * builds tenant-prefixed Redis-backed caches.
 *
 * <p>Caffeine-only options ({@code maximumSize}, {@code expireAfterAccess},
 * {@code initialCapacity}, {@code maximumWeight}) are unsupported and will be
 * logged at WARN level if set.
 */
@Slf4j
public class DynamicRedisCacheManager implements StrategyCacheManager {

    public static final String STRATEGY = "REDIS";

    private final RedisCacheWriter cacheWriter;
    private final RedisCacheConfiguration baseConfiguration;
    private final ConcurrentMap<String, RedisCacheConfiguration> cacheConfigurations = new ConcurrentHashMap<>();

    private volatile RedisCacheManager delegate;

    public DynamicRedisCacheManager(RedisConnectionFactory connectionFactory) {
        this(RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory),
            RedisCacheConfiguration.defaultCacheConfig());
    }

    public DynamicRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration baseConfiguration) {
        this.cacheWriter = cacheWriter;
        this.baseConfiguration = baseConfiguration;
        this.delegate = build(Map.of());
    }

    @Override
    public String getStrategyName() {
        return STRATEGY;
    }

    @Override
    public Set<String> applyTenantConfig(InitCachesEvent event) {
        final String tenantName = event.getTenantKey().toUpperCase();
        final String prefix = TenantCacheManager.buildPrefix(tenantName);

        Map<String, RedisCacheConfiguration> next = new HashMap<>();
        for (XmCacheConfiguration cfg : event.getCacheList()) {
            if (!isOwnedStrategy(cfg)) {
                continue;
            }
            String key = TenantCacheManager.buildKey(tenantName, cfg.getCacheName());
            next.put(key, buildConfiguration(cfg));
        }

        cacheConfigurations.putAll(next);
        this.delegate = build(cacheConfigurations);

        return cacheConfigurations.keySet().stream()
            .filter(it -> it.startsWith(prefix) && !next.containsKey(it)).collect(toSet());
    }

    @Override
    public void cleanTenantConfig(Set<String> cacheKeys) {
        cacheKeys.forEach(cacheConfigurations::remove);
        this.delegate = build(cacheConfigurations);
    }

    @Override
    public Cache getCache(String name) {
        if (!cacheConfigurations.containsKey(name)) {
            throw new IllegalStateException("Cache with name " + name + " did not exist in cache mapping");
        }
        return delegate.getCache(name);
    }

    @Override
    public Collection<String> getCacheNames() {
        return List.copyOf(cacheConfigurations.keySet());
    }

    private RedisCacheManager build(Map<String, RedisCacheConfiguration> configs) {
        RedisCacheManager mgr = RedisCacheManager.builder(cacheWriter)
            .cacheDefaults(baseConfiguration)
            .withInitialCacheConfigurations(configs)
            .disableCreateOnMissingCache()
            .build();
        mgr.afterPropertiesSet();
        return mgr;
    }

    private RedisCacheConfiguration buildConfiguration(XmCacheConfiguration cfg) {
        RedisCacheConfiguration cacheConfig = baseConfiguration;

        if (!CACHE_DEFAULTS.equals(cfg.getExpireAfterWrite())) {
            cacheConfig = cacheConfig.entryTtl(Duration.ofSeconds(cfg.getExpireAfterWrite()));
        }

        warnUnsupported(cfg, "maximumSize", cfg.getMaximumSize());
        warnUnsupported(cfg, "expireAfterAccess", cfg.getExpireAfterAccess());
        warnUnsupported(cfg, "initialCapacity", cfg.getInitialCapacity());
        warnUnsupported(cfg, "maximumWeight", cfg.getMaximumWeight());

        return cacheConfig;
    }

    private static void warnUnsupported(XmCacheConfiguration cfg, String field, Integer value) {
        if (value != null && !CACHE_DEFAULTS.equals(value)) {
            log.warn("Cache[{}] strategy=REDIS does not support '{}' (value={}); ignoring",
                cfg.getCacheName(), field, value);
        }
    }

    private static boolean isOwnedStrategy(XmCacheConfiguration cfg) {
        String s = cfg.getStrategy();
        return s != null && STRATEGY.equalsIgnoreCase(s.trim());
    }
}
