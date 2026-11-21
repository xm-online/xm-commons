# xm-commons-config

## Applied config version API

Reports which xm-ms-config version (commit) the service runs on and a SHA-256 hash of every configuration it applied,
per tenant key (`commons` for configs outside a tenant) with one aggregate hash per key.

- `GET /api/applied-config[?tenant=X][&details=true]`: `details=true` adds the per-path list `{path, hashSum}`.
- Privilege `APPLIED_CONFIG.GET`. Without the management role (`application.management.access.role`, default
  `SYSTEM-ADMIN`) only the token's tenant and `commons` are returned; another tenant is refused.
- On by default; disable with `application.applied-config.enabled=false`.
- The same data without access rules: inject the `AppliedConfigService` bean in code and LEPs.
- Aggregate: SHA-256 hex of the lines `path + ":" + hashSum + "\n"` sorted by path.
