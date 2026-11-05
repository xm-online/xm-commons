# xm-commons

XME Platform - Common Utils and Endpoints

## Use commons for local development in other MS

1. Increase xm-commons version in gradle/version.gradle
2. Deploy to local maven repo with command:
    ```shell script
    ./gradlew clean publishToMavenLocal
    ```
3. Change xm-commons version in target MS in gradle.properties (for example entity).
4. Start target MS.

## Applied config version API

`xm-commons-config` reports, per service, which xm-ms-config version the service runs on and a hash of every
configuration it applied. It is on by default; disable it with `application.applied-config.enabled=false`.

### Endpoint

`GET /api/applied-config[?tenant=X][&details=true]`

```json
{
  "commit": {"mainVersion": "3f2a...", "externalTenantVersions": {"TENANTX": "9b1c..."}, "raw": "{...}",
             "source": "event", "updatedAt": "2026-11-04T10:15:00Z"},
  "fetchScope": {"fetchAll": false, "msConfigPatterns": ["/config/tenants/{tenantName}/entity/**", "..."]},
  "hashes": {"TENANTX": {"aggregate": "c4e2...", "configCount": 57}, "commons": {"aggregate": "77d0...", "configCount": 12}},
  "total": {"aggregate": "...", "configCount": 69},
  "configurationsHashSum": [{"path": "/config/tenants/TENANTX/entity/a.yml", "hashSum": "..."}]
}
```

- `total` is returned to the management role only. `configurationsHashSum` only with `details=true`, in the shape of
  xm-ms-config `ConfigurationsHashSumDto`, limited to the same scope as `hashes`.
- Access is the privilege `APPLIED_CONFIG.GET` (`@PreAuthorize` on `AppliedConfigResource`, checked by
  `hasPermission`; SUPER-ADMIN passes it by itself). Data scope is a separate rule in the service: a caller without
  the management role sees its own tenant and `commons` only. From `externalTenantVersions` it sees only its own
  entry, and `raw` is omitted when it would carry other tenants. `?tenant=<other>` gives 403.
- The management role (`application.management.access.role`, default `SYSTEM-ADMIN`) sees every tenant key,
  `total`, the full `externalTenantVersions` and `raw`. `?tenant=X` narrows `hashes` to `X` and `commons`. It still
  needs the privilege to reach the endpoint.
- Why REST under `/api`, not an actuator endpoint: `/management/**` is management-role only, and in prod it is on a
  separate port, so tenant-scoped callers could not reach it.
- The same data is available in code and LEPs through the `AppliedConfigService` bean (no access rules).

### Security

`AppliedConfigResource` is a `@RestController` registered by the services' component scan of `com.icthh.xm`, so it
works in any service whatever its `SecurityFilterChain`; there is no URL rule for it in `SecurityConfiguration`. The
privilege `APPLIED_CONFIG.GET` is found by the permission scanner from `@PreAuthorize` (without a
`@PrivilegeDescription`: that annotation lives in xm-commons-permission, which depends on this module); grant it to
roles in `permissions.yml`. A service that does not scan `com.icthh.xm` does not
get the endpoint. START/STOP of the call are logged by `RestLoggingAspect`, without the result.

### Commit

- Recorded from every config event, also when none of its paths concern this service, but only after the
  configurations of the event were applied. A failed event does not change it.
- `source`: `startup` (asked from ms-config before the initial load) or `event`. `updatedAt`: when the current
  version was first recorded; the same version in a later event does not change them.
- `unknown`: no version known yet, FILE mode, or a repository without versions.
- `mainVersion` is null and only `raw` is set when the commit is not a plain string and not a `ConfigVersion` JSON.
- Can be stale: a missed event (offset reset, consumer down) leaves the commit older than the applied content;
  ms-config in-memory updates send `getLastVersion()` although their content is not in git at that version (for
  example JWK key rotation). The per-path hashes are the check for that.
- Can be stale: an event with an empty commit (ms-config sends one when a configuration is deleted) is ignored, so
  the commit stays at the previous version until the next event with a commit.

### Expected ms-config endpoint

For the startup value commons calls `GET {xm-config.xm-config-url}/api/private/v2/version` with the header
`X-Config-Token` (the same token as for `config_map`). The endpoint must answer 200 `application/json` with the
same JSON as `ConfigEvent.commit` (`{"mainVersion": "...", "externalTenantVersions": {...}}`); a plain-string body
also works. It does not exist in ms-config yet:

- 404 or 401 (today's ms-config answers 401 on unknown `/api/**` paths): one INFO line, the commit stays `unknown`
  until the first event.
- Any other failure (403, 5xx, timeout): one WARN line, startup continues. No retry; the call uses the config
  rest template timeouts, the same bound as the `config_map` load that follows.
- `migration` profile and FILE mode: no call.
- ms-config keeps its `VersionCache` per replica, so the startup value can lag behind another replica until the
  next event.

### Hashes

- Per path: `sha256Hex(content)` (lowercase hex of SHA-256 of the UTF-8 content), the same as xm-ms-config
  `ConfigurationService.calculateHash`.
- Tenant key: `/config/tenants/{TENANT}/...` gives `TENANT`; `/config/tenants/commons/**` and every path outside a
  tenant give `commons`; `/config/tenants/{TENANT}/commons/**` belongs to `TENANT`.
- Aggregate of a `{path, hashSum}` list: SHA-256 hex of the UTF-8 text made of the lines
  `path + ":" + hashSum + "\n"`, sorted by path in UTF-8 byte order, nothing added after the last line.
  Sort by path, not by whole line: `:` sorts after `-`, `.` and `/`. Check from a `details=true` response:

  ```
  jq -r '.configurationsHashSum | sort_by(.path)[] | "\(.path):\(.hashSum)"' response.json | sha256sum
  ```

  (`jq` sorts by code point, as needed; filter the list to one tenant key first to check that key.)
- "Applied" means delivered to this service's configuration listeners, on the initial load and on refresh. A deleted
  config is removed. Only hashes are stored, updated per changed path; aggregates are computed on request from the
  stored hashes.

### Comparing with xm-ms-config

The aggregate compares replicas of one service and one service over time. It is not comparable with
xm-ms-config as is: ms-config lists all configs of a tenant, a service holds only the paths matching its fetch scope.

1. Take the ms-config list: `/configs_hash` for the tenant of the token, and
   `GET /api/config/tenants/commons/hash` (privilege `CONFIG.ADMIN.GET_HASH_SUM`) for `commons`; `/configs_hash` does
   not include `commons` for a tenant token.
2. Unless `fetchScope.fetchAll` is true, keep the paths that match `fetchScope.msConfigPatterns` (Ant patterns,
   `{tenantName}` matches one segment).
3. Apply the aggregate formula. A path in the filtered list but missing from the service was not applied.

Limit: the service receives processed content (`/config_map*` uses processed configs), while `/configs_hash` hashes
the raw in-memory content. Paths that an ms-config processor changes or generates differ even when in sync; no
ms-config endpoint returns hashes of processed content today. Paths outside `/config/tenants/` are grouped under
`commons` by the service, but ms-config hash lists never contain them.
