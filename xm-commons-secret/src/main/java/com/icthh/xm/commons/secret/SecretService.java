package com.icthh.xm.commons.secret;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.BusinessNotFoundException;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

/**
 * Secrets of the current tenant. Not a {@code @Service}: the logging aspect must never print values, and a tenant
 * logging config could switch that back on.
 */
@Slf4j
@Component
@IgnoreLogginAspect
public class SecretService {

    private static final String REFERENCE_PREFIX = "secret:";
    static final String ERROR_ALIAS_INVALID = "error.secret.alias.invalid";
    static final String ERROR_DATA_INVALID = "error.secret.data.invalid";
    static final String ERROR_REFERENCE_INVALID = "error.secret.reference.invalid";
    static final String ERROR_FIELD_REQUIRED = "error.secret.field.required";
    static final String ERROR_NOT_FOUND = "error.secret.not.found";
    static final String ERROR_READ_DENIED = "error.secret.read.denied";

    private static final int MAX_ALIAS_LENGTH = 256;
    private static final Pattern SEGMENT = Pattern.compile("[A-Za-z0-9_.-]+");
    private static final String PACKAGE = SecretService.class.getPackageName();
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private final TenantContextHolder tenantContextHolder;
    private final ObjectProvider<SecretStore> storeProvider;
    private final Cache<String, Map<String, String>> cache;

    public SecretService(TenantContextHolder tenantContextHolder,
                         SecretProperties properties,
                         ObjectProvider<SecretStore> storeProvider) {
        this.tenantContextHolder = tenantContextHolder;
        this.storeProvider = storeProvider;
        this.cache = Caffeine.newBuilder().expireAfterWrite(properties.getCacheTtl()).build();
    }

    /** Creates or replaces the secret and returns its reference {@code secret:<alias>}. */
    public String put(String alias, Map<String, String> data) {
        return put(alias, data, javaCaller());
    }

    public Map<String, String> get(String alias) {
        return getAs(alias, javaCaller());
    }

    public String get(String alias, String field) {
        return audited("get", alias, javaCaller(), (store, key) -> field(alias, field, read(store, key, alias)));
    }

    public void delete(String alias) {
        delete(alias, javaCaller());
    }

    /**
     * Returns the secret value when {@code value} is a reference {@code secret:<alias>#<field>} (or
     * {@code secret:<alias>} for a secret with one field), otherwise {@code value} unchanged.
     */
    public String resolve(String value) {
        if (value == null || !value.startsWith(REFERENCE_PREFIX)) {
            return value;
        }
        String reference = value.substring(REFERENCE_PREFIX.length());
        int hash = reference.indexOf('#');
        String alias = hash < 0 ? reference : reference.substring(0, hash);
        String field = hash < 0 ? null : reference.substring(hash + 1);
        if (field != null && field.isEmpty()) {
            throw new BusinessException(ERROR_REFERENCE_INVALID, "Invalid secret reference");
        }
        return audited("resolve", alias, javaCaller(), (store, key) -> {
            Map<String, String> data = read(store, key, alias);
            return field == null ? onlyValue(alias, data) : field(alias, field, data);
        });
    }

    String put(String alias, Map<String, String> data, String caller) {
        return audited("put", alias, caller, (store, key) -> {
            checkData(data);
            try {
                store.write(key, data);
            } finally {
                cache.invalidate(key);
            }
            return REFERENCE_PREFIX + alias;
        });
    }

    Map<String, String> getAs(String alias, String caller) {
        return audited("get", alias, caller, (store, key) -> read(store, key, alias));
    }

    void delete(String alias, String caller) {
        audited("delete", alias, caller, (store, key) -> {
            try {
                store.delete(key);
            } finally {
                cache.invalidate(key);
            }
            return null;
        });
    }

    /** Writes the DENIED audit line and returns the error to throw: a LEP read blocked by the blacklist. */
    BusinessException denied(String alias, String caller) {
        boolean valid = isValidAlias(alias);
        audit("get", tenant(), valid ? alias : "-", caller, "DENIED");
        String suffix = valid ? ": " + alias : "";
        return new BusinessException(ERROR_READ_DENIED, "Secret is not readable from LEP" + suffix);
    }

    private static boolean isValidAlias(String alias) {
        if (alias == null || alias.isEmpty() || alias.length() > MAX_ALIAS_LENGTH) {
            return false;
        }
        for (String segment : alias.split("/", -1)) {
            if (!SEGMENT.matcher(segment).matches() || ".".equals(segment) || "..".equals(segment)) {
                return false;
            }
        }
        return true;
    }

    private <T> T audited(String operation, String alias, String caller, BiFunction<SecretStore, String, T> action) {
        SecretStore store = storeProvider.getIfAvailable();
        if (store == null) {
            throw new IllegalStateException("Secrets are disabled: set application.secrets.enabled=true");
        }
        String tenant = tenant();
        if (!isValidAlias(alias)) {
            audit(operation, tenant, "-", caller, "INVALID");
            throw new BusinessException(ERROR_ALIAS_INVALID, "Invalid secret alias");
        }
        try {
            T result = action.apply(store, tenant + "/" + alias);
            audit(operation, tenant, alias, caller, "OK");
            return result;
        } catch (BusinessNotFoundException e) {
            audit(operation, tenant, alias, caller, "NOT_FOUND");
            throw e;
        } catch (RuntimeException e) {
            audit(operation, tenant, alias, caller, "ERROR:" + e.getClass().getSimpleName());
            throw e;
        }
    }

    private Map<String, String> read(SecretStore store, String key, String alias) {
        Map<String, String> data = cache.get(key, k -> store.read(k).orElse(null));
        if (data == null) {
            throw new BusinessNotFoundException(ERROR_NOT_FOUND, "Secret not found: " + alias);
        }
        return Collections.unmodifiableMap(data);
    }

    private static String field(String alias, String field, Map<String, String> data) {
        String value = data.get(field);
        if (value == null) {
            throw new BusinessNotFoundException(ERROR_NOT_FOUND, "Secret field not found: " + alias + "#" + field);
        }
        return value;
    }

    private static String onlyValue(String alias, Map<String, String> data) {
        if (data.size() != 1) {
            throw new BusinessException(ERROR_FIELD_REQUIRED,
                "Secret has several fields, use secret:" + alias + "#<field>");
        }
        return data.values().iterator().next();
    }

    private static void checkData(Map<String, String> data) {
        boolean invalid = data == null || data.isEmpty()
            || data.entrySet().stream().anyMatch(e -> e.getKey() == null || e.getValue() == null);
        if (invalid) {
            throw new BusinessException(ERROR_DATA_INVALID, "Secret data must have at least one field and no nulls");
        }
    }

    private String tenant() {
        return TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder).toUpperCase();
    }

    /** Walks the stack lazily, returns the first class outside this package: the service that called SecretService. */
    private static String javaCaller() {
        return WALKER.walk(frames -> frames
            .map(StackWalker.StackFrame::getDeclaringClass)
            .filter(type -> !type.getPackageName().equals(PACKAGE))
            .findFirst()
            .map(Class::getName)
            .orElse("unknown"));
    }

    private static void audit(String operation, String tenant, String alias, String caller, String result) {
        log.info("secret audit: operation={}, tenant={}, alias={}, caller={}, result={}",
            operation, tenant, alias, caller, result);
    }
}
