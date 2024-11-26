package com.icthh.xm.commons.service.impl;

import com.icthh.xm.commons.config.FunctionApiSpecConfiguration;
import com.icthh.xm.commons.domain.enums.FunctionTxTypes;
import com.icthh.xm.commons.domain.spec.FunctionApiSpec;
import com.icthh.xm.commons.permission.service.DynamicPermissionCheckService;
import com.icthh.xm.commons.service.FunctionService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.utils.CollectionsUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Map;

import static com.icthh.xm.commons.utils.JsonValidationUtils.assertJson;

@Service
@ConditionalOnMissingBean(FunctionService.class)
public class FunctionServiceImpl extends AbstractFunctionService<FunctionApiSpec> {

    private final TenantContextHolder tenantContextHolder;
    private final FunctionApiSpecConfiguration functionApiSpecConfiguration;
    private final AntPathMatcher matcher;

    public FunctionServiceImpl(DynamicPermissionCheckService dynamicPermissionCheckService,
                               TenantContextHolder tenantContextHolder,
                               FunctionApiSpecConfiguration functionApiSpecConfiguration) {
        super(dynamicPermissionCheckService);
        this.tenantContextHolder = tenantContextHolder;
        this.functionApiSpecConfiguration = functionApiSpecConfiguration;
        this.matcher = new AntPathMatcher();
    }

    @Override
    public FunctionApiSpec findFunctionSpec(String functionKey, String httpMethod) {
        String tenantKey = tenantContextHolder.getTenantKey();
        return functionApiSpecConfiguration.getSpecByTenant(tenantKey)
            .orElseThrow(() -> new IllegalStateException(
                String.format("Function by key: %s and tenant: %s not found", functionKey, tenantKey)));
    }

    @Override
    public Map<String, Object> getValidFunctionInput(FunctionApiSpec functionSpec, Map<String, Object> functionInput) {
        Map<String, Object> input = CollectionsUtils.getOrEmpty(functionInput);
        if (functionSpec.isValidateFunctionInput()) {
            assertJson(functionInput, functionSpec.getInputSpec());
        }
        return input;
    }

    @Override
    public void enrichInputFromPathParams(String functionKey, Map<String, Object> functionInput, FunctionApiSpec functionSpec) {
        if (functionSpec.getPath() != null && matcher.match(functionSpec.getPath(), functionKey)) {
            functionInput.putAll(matcher.extractUriTemplateVariables(functionSpec.getPath(), functionKey));
        }
    }

    @Override
    public boolean isAnonymous(FunctionApiSpec functionSpec) {
        return functionSpec.getAnonymous();
    }

    @Override
    public FunctionTxTypes getTxType(FunctionApiSpec functionSpec) {
        return functionSpec.getTxType();
    }
}
