package com.icthh.xm.commons.permission.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("xm-permission")
@Getter
@Setter
public class PermissionProperties {

    private static final String DEFAULT_PRIVILEGES_SPEC = "/config/tenants/privileges.yml";
    private static final String DEFAULT_PERMISSIONS_SPEC = "/config/tenants/{tenantName}/permissions.yml";
    private static final String DEFAULT_ROLES_SPEC = "/config/tenants/{tenantName}/roles.yml";
    private static final String DEFAULT_ENV_SPEC = "/config/tenants/environments.yml";

    private String privilegesSpecPath = DEFAULT_PRIVILEGES_SPEC;
    private String permissionsSpecPath = DEFAULT_PERMISSIONS_SPEC;
    private String rolesSpecPath = DEFAULT_ROLES_SPEC;
    private String envSpecPath = DEFAULT_ENV_SPEC;
}
