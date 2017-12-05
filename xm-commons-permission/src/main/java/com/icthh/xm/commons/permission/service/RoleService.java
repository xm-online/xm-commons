package com.icthh.xm.commons.permission.service;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.config.PermissionProperties;
import com.icthh.xm.commons.permission.domain.Role;
import com.icthh.xm.commons.permission.domain.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@IgnoreLogginAspect
public class RoleService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";
    private ConcurrentHashMap<String, Map<String, Role>> roles = new ConcurrentHashMap<>();
    private final AntPathMatcher matcher = new AntPathMatcher();

    private final PermissionProperties permissionProperties;

    /**
     * Get roles configuration for tenant.
     * Map key is ROLE_KEY and value is role.
     *
     * @param tenant the tenant
     * @return role
     */
    public Map<String, Role> getRoles(String tenant) {
        if (!roles.containsKey(tenant)) {
            return new HashMap<>();
        }
        return roles.get(tenant);
    }

    @Override
    public void onRefresh(String key, String config) {
        try {
            String tenant = matcher.extractUriTemplateVariables(permissionProperties
                .getRolesSpecPath(), key).get(TENANT_NAME);
            if (StringUtils.isBlank(config)) {
                roles.remove(tenant);
                log.info("Role specification for tenant {} was removed", tenant);
            } else {
                roles.put(tenant, RoleMapper.ymlToRoles(config));
                log.info("Role specification for tenant {} was updated", tenant);
            }
        } catch (Exception e) {
            log.error("Error read role specification from path " + key, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(String key) {
        return matcher.match(permissionProperties.getRolesSpecPath(), key);
    }

    @Override
    public void onInit(String key, String config) {
        onRefresh(key, config);
    }
}
