# order.yml — configuration refresh order

Controls the order in which config files are delivered to a microservice
(`onRefresh` / `onInit` / `refreshFinished`) — on startup and on config update events.

## Location

One file per tenant, in the tenant root:

```
/config/tenants/{TENANT}/order.yml
```

## Structure

A single `order` key with a list of [Ant patterns](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/util/AntPathMatcher.html),
relative to the tenant root:

```yaml
order:
  - tenant-config.yml
  - entity/xmentityspec/*.yml
  - lep/**
```

## Rules

1. `order.yml` itself is always delivered first among that tenant's files in the
   batch — a change to it applies to its own batch. "First" is scoped to the
   tenant: other tenants' files and non-tenant paths keep their exact positions
   in the batch (no cross-tenant ordering guarantee).
2. Then files matching pattern 1, then pattern 2, ... (first matching pattern wins).
3. Unlisted files go last, keeping their original order.
4. Ordering is per tenant. No `order.yml` — no reordering for that tenant.
5. Broken `order.yml` never breaks refresh: parse errors keep the previous order.
6. Deleting `order.yml` removes that tenant's ordering and restores natural
   (unordered, original delivery-order) behavior — the same as if `order.yml`
   had never existed for that tenant.

## Example

`/config/tenants/XM/order.yml`:

```yaml
order:
  - tenant-config.yml
  - uaa/**
```

Update event arrives with:

```
/config/tenants/XM/webapp/settings-public.yml
/config/tenants/XM/uaa/uaa.yml
/config/tenants/XM/order.yml
/config/tenants/XM/tenant-config.yml
```

Delivery order:

```
/config/tenants/XM/order.yml            # always first
/config/tenants/XM/tenant-config.yml    # pattern 1
/config/tenants/XM/uaa/uaa.yml          # pattern 2
/config/tenants/XM/webapp/settings-public.yml  # unlisted — last
```

## Pattern syntax

| Pattern | Matches |
|---------|---------|
| `tenant-config.yml` | exactly that file |
| `entity/*.yml` | yml files directly in `entity/` |
| `lep/**` | everything under `lep/`, recursively |
| `**/dashboard-*.yml` | `dashboard-*.yml` at any depth |
