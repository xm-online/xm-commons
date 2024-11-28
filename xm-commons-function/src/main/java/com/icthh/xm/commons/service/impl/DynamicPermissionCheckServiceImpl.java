package com.icthh.xm.commons.service.impl;

import com.icthh.xm.commons.config.FunctionApiSpecConfiguration;
import com.icthh.xm.commons.domain.spec.FunctionApiSpec;
import com.icthh.xm.commons.permission.service.AbstractDynamicPermissionCheckService;
import com.icthh.xm.commons.permission.service.DynamicPermissionCheckService;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(DynamicPermissionCheckService.class)
public class DynamicPermissionCheckServiceImpl extends AbstractDynamicPermissionCheckService {

    private final TenantContextHolder tenantContextHolder;
    private final FunctionApiSpecConfiguration functionApiSpecConfiguration;

    public DynamicPermissionCheckServiceImpl(PermissionCheckService permissionCheckService,
                                             XmAuthenticationContextHolder xmAuthenticationContextHolder,
                                             TenantContextHolder tenantContextHolder,
                                             FunctionApiSpecConfiguration functionApiSpecConfiguration) {
        super(permissionCheckService, xmAuthenticationContextHolder);
        this.tenantContextHolder = tenantContextHolder;
        this.functionApiSpecConfiguration = functionApiSpecConfiguration;
    }

    public Boolean isDynamicFunctionPermissionEnabled() {
        String tenantKey = tenantContextHolder.getTenantKey();
        return functionApiSpecConfiguration.getSpecByTenant(tenantKey)
            .map(FunctionApiSpec::isDynamicPermissionCheckEnabled)
            .orElse(Boolean.FALSE);
    }
}
