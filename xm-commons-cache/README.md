## xm-commons-cache
XM^online commons project for cache implementation.

This functionality allows to use Caffeine cache in lep environment.

Cache configuration for tenant is stored in cache.yml
```yaml
---
- cacheName: "CacheName1"
  strategy: "CAFFEINE"   # optional, default CAFFEINE
  initialCapacity: 10
  maximumSize: 100
  expireAfterWrite: 60
  expireAfterAccess: 60
  recordStats: true
```
Settings works same as Caffeine Cache Settings.

### Strategies

Each cache entry may declare a `strategy`. The default is `CAFFEINE`. The
`REDIS` strategy is also bundled (see below). New strategies are pluggable:
any Spring bean implementing `StrategyCacheManager` is auto-discovered by
`DynamicTenantCacheManager`, which routes per-cache requests to the matching
strategy implementation.

### REDIS strategy

Caches declared with `strategy: "REDIS"` are backed by Spring Data Redis.
The Redis support is bundled directly into this module (previously a separate
subproject). `spring-data-redis` and `lettuce-core` are pulled in transitively
as `api` dependencies — no extra dependency is required in the consumer app.

Note: this module intentionally does **not** depend on
`spring-boot-starter-data-redis`, so Spring Boot's `RedisAutoConfiguration` is
**not** activated. The Redis connection factory is created only by this
module's own auto-configuration, gated by
`@ConditionalOnClass(RedisConnectionFactory)` and the property below.

Enable it via:
```yaml
application:
  tenant-memory-cache:
    enabled: true
  tenant-cache:
    redis:
      enabled: true
      host: localhost
      port: 6379
      database: 0
      password: ""       # optional
```

When activated, this module:
1. Creates a `LettuceConnectionFactory` from the properties above (skipped if
   another `RedisConnectionFactory` bean is already present in the context).
2. Registers `DynamicRedisCacheManager` as a `StrategyCacheManager` for
   strategy `REDIS`. `DynamicTenantCacheManager` automatically picks it up
   and routes Redis-strategy caches to it.

Example mixed `cache.yml`:
```yaml
---
- cacheName: "InMemoryCache"
  strategy: "CAFFEINE"     # optional, default
  maximumSize: 100
  expireAfterWrite: 60

- cacheName: "SharedCache"
  strategy: "REDIS"
  expireAfterWrite: 300    # mapped to Redis entry TTL (seconds)
```

Redis caches honor `expireAfterWrite` only. The following Caffeine-only fields
are ignored (a WARN is logged if set): `maximumSize`, `expireAfterAccess`,
`initialCapacity`, `maximumWeight`.

Once created cache could not be destroyed in runtime after create. Cache will be purged but not destroyed.
```cache.yml``` update will trigger purge of all tenant caches 
