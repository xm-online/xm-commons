package com.icthh.xm.commons.secret;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.YamlMapperUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-tenant secrets settings from {@code /config/tenants/{tenantName}/secrets.yml}, shared by all services.
 * A file that cannot be parsed marks the tenant invalid: the blacklist then fails closed instead of being dropped.
 */
@Slf4j
@Component
public class TenantSecretsConfiguration implements RefreshableConfiguration {

    private static final String PATH = "/config/tenants/{tenantName}/secrets.yml";
    private static final String TENANT_NAME = "tenantName";

    private static final Blacklist INVALID = new Blacklist(List.of(), true);

    /** Unknown keys fail: a typo or a wrong layout must not leave the tenant without its blacklist. */
    private final ObjectMapper mapper =
        YamlMapperUtils.yamlDeserializationMapper(Map.of(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true));
    private final AntPathMatcher matcher = new AntPathMatcher();
    /** One entry per tenant, replaced in one step, so a reader never sees a half-applied refresh. */
    private final Map<String, Blacklist> blacklists = new ConcurrentHashMap<>();
    private final TenantContextHolder tenantContextHolder;

    TenantSecretsConfiguration(TenantContextHolder tenantContextHolder) {
        this.tenantContextHolder = tenantContextHolder;
    }

    /** Read blacklist patterns of the current tenant; throws when the tenant's file is malformed. */
    List<String> lepReadBlacklist() {
        String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder).toUpperCase();
        Blacklist blacklist = blacklists.get(tenant);
        if (blacklist == null) {
            return List.of();
        }
        if (blacklist.invalid()) {
            throw new BusinessException(LepSecrets.ERROR_BLACKLIST_INVALID,
                "secrets.yml must hold only lepReadBlacklist, a list of strings");
        }
        return blacklist.patterns();
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        String tenant = matcher.extractUriTemplateVariables(PATH, updatedKey).get(TENANT_NAME).toUpperCase();
        if (StringUtils.isBlank(config)) {
            blacklists.remove(tenant);
        } else {
            blacklists.put(tenant, parse(tenant, config));
        }
    }

    private Blacklist parse(String tenant, String config) {
        try {
            List<String> patterns = mapper.readValue(config, Settings.class).getLepReadBlacklist();
            return new Blacklist(patterns == null ? List.of() : List.copyOf(patterns), false);
        } catch (Exception e) {
            log.error("Tenant {} secrets configuration is invalid, LEP secret reads are refused: {}",
                tenant, e.getMessage());
            return INVALID;
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(PATH, updatedKey);
    }

    private record Blacklist(List<String> patterns, boolean invalid) {
    }

    @Data
    static class Settings {
        private List<String> lepReadBlacklist;
    }
}
