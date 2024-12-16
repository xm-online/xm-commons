package com.icthh.xm.commons.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.permission.service.AbstractDynamicPermissionCheckService;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.commons.utils.Constants.FUNCTIONS;
import static com.icthh.xm.commons.utils.Constants.TENANT_CONFIG_DYNAMIC_CHECK_ENABLED;

@Component("dynamicPermissionCheckService")
public class DynamicPermissionCheckServiceImpl extends AbstractDynamicPermissionCheckService {

    private final TenantConfigService tenantConfigService;
    private final ObjectMapper mapper;

    public DynamicPermissionCheckServiceImpl(PermissionCheckService permissionCheckService,
                                             XmAuthenticationContextHolder xmAuthenticationContextHolder,
                                             TenantConfigService tenantConfigService) {
        super(permissionCheckService, xmAuthenticationContextHolder);
        this.tenantConfigService = tenantConfigService;
        this.mapper = new ObjectMapper();
    }


    public Boolean isDynamicFunctionPermissionEnabled() {
        return Optional.ofNullable(tenantConfigService.getConfig())
            .map(c -> c.get(FUNCTIONS))
            .map(c -> mapper.convertValue(c, new TypeReference<Map<String, Object>>() {}))
            .map(f -> Boolean.valueOf(f.get(TENANT_CONFIG_DYNAMIC_CHECK_ENABLED).toString()))
            .orElse(Boolean.FALSE);
    }
}
