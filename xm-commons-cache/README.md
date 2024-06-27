## xm-commons-cache
XM^online commons project for cache implementation.

This functionality allows to use Caffeine cache in lep environment.

Cache configuration for tenant is stored in cache.yml
```yaml
---
- cacheName: "CacheName1"
  initialCapacity: 10
  maximumSize: 100
  expireAfterWrite: 60
  expireAfterAccess: 60
  recordStats: true
```
Settings works same as Caffeine Cache Settings.

Once created cache could not be destroyed in runtime after create. Cache will be purged but not destroyed.
```cache.yml``` update will trigger purge of all tenant caches 
