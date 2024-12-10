package com.icthh.xm.commons.service.impl;

import com.icthh.xm.commons.config.FunctionApiSpecConfiguration;
import com.icthh.xm.commons.domain.spec.FunctionSpec;
import com.icthh.xm.commons.permission.service.DynamicPermissionCheckService;
import com.icthh.xm.commons.service.FunctionService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.utils.CollectionsUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.icthh.xm.commons.utils.JsonValidationUtils.assertJson;
import static java.lang.Boolean.TRUE;

@Service
@ConditionalOnMissingBean(FunctionService.class)
public class FunctionServiceImpl extends AbstractFunctionService<FunctionSpec> {

    private final TenantContextHolder tenantContextHolder;
    private final FunctionApiSpecConfiguration functionApiSpecConfiguration;

    public FunctionServiceImpl(DynamicPermissionCheckService dynamicPermissionCheckService,
                               TenantContextHolder tenantContextHolder,
                               FunctionApiSpecConfiguration functionApiSpecConfiguration) {
        super(dynamicPermissionCheckService);
        this.tenantContextHolder = tenantContextHolder;
        this.functionApiSpecConfiguration = functionApiSpecConfiguration;
    }

    @Override
    public FunctionSpec findFunctionSpec(String functionKey, String httpMethod) {
        String tenantKey = tenantContextHolder.getTenantKey();
        return functionApiSpecConfiguration.getSpecByKeyAndTenant(functionKey, tenantKey)
            .orElseThrow(() -> new IllegalStateException(
                String.format("Function by key: %s and tenant: %s not found", functionKey, tenantKey)));
    }

    @Override
    public Map<String, Object> getValidFunctionInput(FunctionSpec functionSpec, Map<String, Object> functionInput) {
        Map<String, Object> input = CollectionsUtils.getOrEmpty(functionInput);
        if (TRUE.equals(functionSpec.getValidateFunctionInput())) {
            assertJson(functionInput, functionSpec.getInputSpec());
        }
        return input;
    }
}
