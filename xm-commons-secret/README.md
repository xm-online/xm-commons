# xm-commons-secret

Tenant secrets in Vault (KV v2), for Java code (`SecretService`) and LEP (`lepContext.secrets`).
Add the module as a dependency. It is off until `application.secrets.enabled=true`: the beans exist, every call fails
with "Secrets are disabled", and a service that does not use secrets starts as before.

## Settings

| Property (`application.secrets.`) | Default | Meaning |
|---|---|---|
| `enabled` | `false` | Turns the Vault store on |
| `url` | none | Vault base URL, required when enabled |
| `mount` | `xm` | KV v2 mount |
| `authentication` | `kubernetes` | `kubernetes` or `token` (local dev) |
| `kubernetes.role` | none | Vault role |
| `kubernetes.service-account-token-path` | `/var/run/secrets/kubernetes.io/serviceaccount/token` | Token file |
| `kubernetes.path` | `kubernetes` | Vault auth mount |
| `token` | none | Static token for `authentication: token` |
| `cache-ttl` | `60s` | Read cache per tenant and alias; `0` turns it off |
| `lep-read-blacklist` | `[]` | Ant patterns LEP may not read |

```yaml
application:
  secrets:
    enabled: true
    url: https://vault.example.com:8200
    kubernetes:
      role: my-service
    lep-read-blacklist:
      - integrations/**
```

Local development with a static token:

```yaml
application:
  secrets:
    enabled: true
    url: http://localhost:8200
    authentication: token
    token: dev-root-token
```

## Alias and storage path

An alias is segments `[A-Za-z0-9_.-]+` separated by `/`: no empty segment, no `.` or `..` segment, at most 256
characters. Anything else is rejected (`error.secret.alias.invalid`) before the store is called.
The path is `<mount>/data/<TENANT>/<alias>`; the caller never passes the tenant. No tenant in context fails.
`put` and `delete` evict the cache entry of this instance; other instances see the change after `cache-ttl`.
`delete` is a KV v2 soft delete: old versions stay in Vault.

## Java

```java
String ref = secretService.put("integrations/crm", Map.of("token", "..."));  // "secret:integrations/crm"
String token = secretService.get("integrations/crm", "token");
String value = secretService.resolve(configValue); // "secret:integrations/crm#token" -> value; other text unchanged
```

Reference format: `secret:<alias>#<field>`, or `secret:<alias>` when the secret has exactly one field
(several fields: `error.secret.field.required`). A value starting with `secret:` that is malformed fails instead of being
returned as text. Java `get` and `resolve` ignore the blacklist.

## LEP

```groovy
// store a token entered in a form: LEP may write any alias
lepContext.secrets.put("integrations/crm", [token: lepContext.inArgs.token])
// read: refused for blacklisted aliases
def data = lepContext.secrets.get("crm-public/key")
lepContext.secrets.delete("crm-public/key")
```

`get` is refused (`error.secret.read.denied`) for aliases matching the merged blacklist: the service property
`application.secrets.lep-read-blacklist` plus the tenant's `/config/tenants/{tenantName}/secrets.yml` (one file per tenant, shared by all services), which can
only add patterns:

```yaml
lepReadBlacklist:
  - integrations/**
```

A malformed `secrets.yml` makes every LEP `get` of that tenant fail (`error.secret.blacklist.invalid`) until the file
is fixed or emptied.

## Errors

| Error | Meaning |
|---|---|
| `error.secret.not.found` (404, `BusinessNotFoundException`) | Secret or field missing |
| `SecretStoreException` | Vault unreachable or failed; the cause is kept, no value |
| `error.secret.alias.invalid`, `error.secret.data.invalid`, `error.secret.reference.invalid` | Bad input |
| `error.secret.read.denied` | LEP read of a blacklisted alias |

The module never logs values and never puts them in exception messages. One INFO audit line per operation:
`secret audit: operation, tenant, alias, caller (LEP key or class), result`.

Known limitation: the loggers `org.springframework.web.client.*` and the HTTP client wire logger
(`org.apache.hc.client5.http.wire`) print request and response bodies at DEBUG, secret values and the Kubernetes
login JWT included. Keep them above DEBUG in production.

## Vault policy

Per service, for example `path "xm/data/+/<prefix>/*"` with the capabilities the service needs.

## Trust model

LEP Groovy is not sandboxed: a LEP can switch the tenant. Tenant scoping and the blacklist protect against mistakes,
not against a hostile LEP author. A LEP can also bypass the read
blacklist through `lepContext.secrets.secretService`, because Groovy ignores `private`.
