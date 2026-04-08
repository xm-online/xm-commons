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

Each cache entry may declare a `strategy`. The default is `CAFFEINE`. New
strategies are pluggable: any Spring bean implementing `StrategyCacheManager`
is auto-discovered by `DynamicTenantCacheManager`, which routes per-cache
requests to the matching strategy implementation.

Additional strategies are shipped as separate modules — for example the
`REDIS` strategy lives in [`xm-commons-cache-redis`](../xm-commons-cache-redis).
Add that module to your application to enable Redis-backed caches; this core
module has no dependency on Spring Data Redis.

Once created cache could not be destroyed in runtime after create. Cache will be purged but not destroyed.
```cache.yml``` update will trigger purge of all tenant caches 
