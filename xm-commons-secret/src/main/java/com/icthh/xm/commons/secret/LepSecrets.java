package com.icthh.xm.commons.secret;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.exceptions.BusinessException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final SecretProperties properties;
    private final Optional<TenantConfigService> tenantConfigService;
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
        return alias != null && Stream.concat(properties.getLepReadBlacklist().stream(), tenantBlacklist().stream())
            .anyMatch(pattern -> MATCHER.match(pattern, alias));
    }

    private List<String> tenantBlacklist() {
        Object section = tenantConfigService.map(TenantConfigService::getConfig).map(c -> c.get("secrets")).orElse(null);
        if (section == null) {
            return List.of();
        }
        Object value = section instanceof Map<?, ?> map ? map.get("lepReadBlacklist") : invalid();
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list && list.stream().allMatch(String.class::isInstance)) {
            return list.stream().map(String.class::cast).toList();
        }
        return invalid();
    }

    private static <T> T invalid() {
        throw new BusinessException(ERROR_BLACKLIST_INVALID, "secrets.lepReadBlacklist must be a list of strings");
    }
}
