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
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final ObjectMapper mapper = YamlMapperUtils.yamlDefaultMapper();
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final Map<String, List<String>> blacklists = new ConcurrentHashMap<>();
    private final Set<String> invalid = ConcurrentHashMap.newKeySet();
    private final TenantContextHolder tenantContextHolder;

    TenantSecretsConfiguration(TenantContextHolder tenantContextHolder) {
        this.tenantContextHolder = tenantContextHolder;
    }

    /** Read blacklist patterns of the current tenant; throws when the tenant's file is malformed. */
    List<String> lepReadBlacklist() {
        String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder).toUpperCase();
        if (invalid.contains(tenant)) {
            throw new BusinessException(LepSecrets.ERROR_BLACKLIST_INVALID,
                "secrets.yml lepReadBlacklist must be a list of strings");
        }
        return blacklists.getOrDefault(tenant, List.of());
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        String tenant = matcher.extractUriTemplateVariables(PATH, updatedKey).get(TENANT_NAME).toUpperCase();
        blacklists.remove(tenant);
        invalid.remove(tenant);
        if (StringUtils.isBlank(config)) {
            return;
        }
        try {
            List<String> patterns = mapper.readValue(config, Settings.class).getLepReadBlacklist();
            blacklists.put(tenant, patterns == null ? List.of() : List.copyOf(patterns));
        } catch (Exception e) {
            invalid.add(tenant);
            log.error("Tenant {} secrets configuration is invalid, LEP secret reads are refused: {}",
                tenant, e.getMessage());
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(PATH, updatedKey);
    }

    @Data
    static class Settings {
        private List<String> lepReadBlacklist;
    }
}
