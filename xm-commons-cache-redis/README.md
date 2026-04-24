## xm-commons-cache-redis

Redis strategy for [`xm-commons-cache`](../xm-commons-cache).

Caches declared with `strategy: "REDIS"` in `cache.yml` are backed by Spring
Data Redis. This module depends on `spring-data-redis` and `lettuce-core`
directly and intentionally does **not** pull in
`spring-boot-starter-data-redis`, so Spring Boot's `RedisAutoConfiguration` is
**not** activated — the Redis connection factory is created only by this
module's own auto-configuration, gated by
`@ConditionalOnClass(RedisConnectionFactory)` and the property below.

### Enable it

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
   strategy `REDIS`. `DynamicTenantCacheManager` (from `xm-commons-cache`)
   automatically picks it up and routes Redis-strategy caches to it.

### Example mixed `cache.yml`

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