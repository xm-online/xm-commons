package com.icthh.xm.commons.secret;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * {@code lepContext.secrets}: one instance per LEP call, bound to the LEP key for the audit line. Reads are
 * refused for aliases on the read blacklist; put and delete are not.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class LepSecrets {

    static final String ERROR_BLACKLIST_INVALID = "error.secret.blacklist.invalid";

    private static final AntPathMatcher MATCHER = new AntPathMatcher("/");

    private final SecretService secretService;
    private final List<String> servicePatterns;
    private final TenantSecretsConfiguration tenantSecrets;
    private final String caller;

    /** Creates or replaces the secret and returns its reference {@code secret:<alias>}. */
    public String put(String alias, Map<String, String> data) {
        return secretService.put(alias, data, caller);
    }

    public Map<String, String> get(String alias) {
        if (isBlacklisted(alias)) {
            throw secretService.denied(alias, caller);
        }
        return secretService.getAs(alias, caller);
    }

    public void delete(String alias) {
        secretService.delete(alias, caller);
    }

    private boolean isBlacklisted(String alias) {
        return alias != null && Stream.concat(servicePatterns.stream(), tenantSecrets.lepReadBlacklist().stream())
            .anyMatch(pattern -> MATCHER.match(pattern, alias));
    }
}
