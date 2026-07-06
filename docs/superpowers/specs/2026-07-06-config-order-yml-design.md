# Design: `order.yml` — ordered configuration refresh (xm-commons-config)

**Date:** 2026-07-06
**Status:** Draft — awaiting review
**Module:** `xm-commons-config`

## Problem

When a microservice receives configuration files — on startup or via a config-changed
event — the files are delivered to `RefreshableConfiguration` beans in effectively
undefined order (map iteration order on startup, incoming event order on refresh).
Some configurations depend on others being processed first.

## Goal

A tenant can place an `order.yml` file in its tenant root
(`/config/tenants/{TENANT}/order.yml`) that defines the order in which that tenant's
configuration files are delivered:

1. If `order.yml` exists for a tenant, it is itself delivered first.
2. Files matching its patterns are delivered next, in the listed order.
3. All remaining files follow, keeping their previous relative order.
4. `refreshFinished(paths)` receives paths in the same order (both flows).

Applies to **both** delivery flows:
- **Startup:** `InitRefreshableConfigurationBeanPostProcessor.initBean` → `onInit` per file → `refreshFinished`.
- **Event:** `AbstractConfigService.updateConfigurations` → `onConfigurationChanged` → `onRefresh` per file → `refreshFinished`.

## `order.yml` format

Location: `/config/tenants/{TENANT}/order.yml`. Already covered by the default fetch
patterns (`/config/tenants/{tenantName}/*` in `FetchConfigurationSettings`), so it is
fetched at startup and passes the event-path filter without new patterns.

```yaml
order:
  - tenant-config.yml
  - entity/xmentityspec/*.yml
  - lep/**
```

- Entries are **Ant patterns relative to the tenant root**, matched with
  `AntPathMatcher` (the established idiom in this codebase).
- Each entry forms a priority group; the **first** matching entry wins.
- Unknown top-level keys are ignored (forward compatibility).

## Ordering semantics (per batch)

For each tenant's files within a batch, the sort rank is:

| Rank | Files |
|------|-------|
| 0 | the tenant's `order.yml` itself |
| 1..N | files whose tenant-relative path matches pattern 1..N (first match wins) |
| last | files matching no pattern, in their original relative order |

The sort is **stable**: ties keep their original relative order.

**Cross-tenant:** each tenant's files are permuted only within the index positions
that tenant already occupies in the batch. Files of other tenants and non-tenant
paths keep their exact positions. No cross-tenant ordering guarantee (per decision).
`commons` is treated like any other tenant — an `order.yml` under
`/config/tenants/commons/` works identically.

Tenants without an `order.yml` (or with an unparseable one and no previous valid
version) are left untouched.

## Components

### New: `ConfigurationOrderService`
`com.icthh.xm.commons.config.client.service.ConfigurationOrderService`

- Thread-safe cache: `ConcurrentHashMap<String /*tenant*/, List<String> /*patterns*/>`.
  (Startup seeding and Kafka consumer thread may touch it concurrently.)
- `void processOrderConfigs(Map<String, Configuration> configs)` — scans a fetched
  map for paths matching `/config/tenants/{tenantName}/order.yml`; parses content and
  updates the cache. `null`/blank content (file deleted) removes the tenant's entry.
- `List<String> sortPaths(List<String> paths)` — applies the semantics above;
  returns a new list, never mutates input.
- YAML parsing: Jackson `ObjectMapper` + `YAMLFactory`
  (same idiom as `TenantAliasPreCompileServiceImpl`), into a small spec DTO
  (`List<String> order`), unknown properties ignored.

### Changed: `AbstractConfigService.updateConfigurations`
After fetching the batch's configuration map:
1. `orderService.processOrderConfigs(configurationsMap)` — so an `order.yml` change
   applies to **its own batch**;
2. sort the dispatch list with `sortPaths` before `notifyUpdated` loop;
3. pass the **sorted** filtered paths to `refreshFinished`.

Constructor gains a `ConfigurationOrderService` parameter (propagated to
`CommonConfigService`). This is a source-breaking change for downstream subclasses
of `AbstractConfigService`; precedent exists (`FetchConfigurationSettings` was added
the same way).

### Changed: `InitRefreshableConfigurationBeanPostProcessor`
- After the initial `getConfigMapAntPattern` fetch (`getConfig()`), call
  `processOrderConfigs(configMap)` once — this also seeds the cache used later by
  the event flow.
- `initConfigPaths` iterates keys sorted by `sortPaths` instead of raw map entry
  order, so `onInit` order and the returned path list (fed to `refreshFinished`)
  follow `order.yml`.
- Constructor gains the `ConfigurationOrderService` parameter.

The per-bean `refreshFinished` path lists are derived via order-preserving stream
filters from the globally sorted list, so requirement 4 holds in both flows without
extra work.

### Wiring: `XmConfigConfiguration`
New `@Bean ConfigurationOrderService`, injected into the `configService` bean and
`refreshableConfigurationPostProcessor` bean. It is a plain POJO with no
dependencies, safe to instantiate early with the `BeanPostProcessor`.

## Error handling

- Unparseable `order.yml` → `log.error` with tenant + path; **keep the previous
  cached order** for that tenant.
- Any unexpected exception inside `sortPaths` → log and return the original list.
  A bad `order.yml` must never break configuration refresh.
- Missing `order.yml` → no reordering for that tenant (current behavior).

## Testing

- **`ConfigurationOrderServiceUnitTest`:** pattern-group ordering, first-match-wins,
  unmatched-files-last with stable order, `order.yml` ranks first, per-tenant
  isolation (multi-tenant batch keeps other tenants' positions), invalid YAML keeps
  previous order, deletion clears the tenant entry, non-tenant paths untouched.
- **`AbstractConfigService` (via `CommonConfigService`) test:** listener receives
  `onConfigurationChanged` in sorted order; `refreshFinished` gets the same order;
  an `order.yml` arriving in the batch is applied to that same batch and dispatched
  first.
- **`InitRefreshableConfigurationBeanPostProcessorUnitTest` (extend):** startup
  `onInit` call order and `refreshFinished` path order follow `order.yml`.

## Out of scope

- Cross-tenant ordering guarantees.
- Ordering guarantees between files delivered to *different* microservices.
- Any change to `xm-ms-config` (server side) — this is purely client-side delivery
  order.
