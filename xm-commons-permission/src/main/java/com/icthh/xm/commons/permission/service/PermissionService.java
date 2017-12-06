package com.icthh.xm.commons.permission.service;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.config.PermissionProperties;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.mapper.PermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@IgnoreLogginAspect
public class PermissionService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";
    // root map key is tenant name, value is map with key = "role_key:privilege_key", value = permission
    private ConcurrentHashMap<String, Map<String, Permission>> permissions = new ConcurrentHashMap<>();
    private final AntPathMatcher matcher = new AntPathMatcher();

    private final PermissionProperties permissionProperties;

    @Value("${spring.application.name}")
    private String appName;

    /**
     * Get permissions configuration for tenant.
     * Map key is ROLE_KEY:PRIVILEGE_KEY and value id permission.
     *
     * @param tenant the tenant
     * @return permissions
     */
    public Map<String, Permission> getPermissions(String tenant) {
        if (!permissions.containsKey(tenant)) {
            return new HashMap<>();
        }
        return permissions.get(tenant);
    }

    @Override
    public void onRefresh(String key, String config) {
        try {
            String tenant = matcher.extractUriTemplateVariables(permissionProperties
                .getPermissionsSpecPath(), key).get(TENANT_NAME);
            if (StringUtils.isBlank(config)) {
                permissions.remove(tenant);
                log.info("Permission specification for tenant {} was removed", tenant);
            } else {
                permissions.put(tenant, PermissionMapper.ymlToPermissions(config, appName));
                log.info("Permission specification for tenant {} was updated", tenant);
            }
        } catch (Exception e) {
            log.error("Error read permission specification from path " + key, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(String key) {
        return matcher.match(permissionProperties.getPermissionsSpecPath(), key);
    }

    @Override
    public void onInit(String key, String config) {
        onRefresh(key, config);
    }
}
