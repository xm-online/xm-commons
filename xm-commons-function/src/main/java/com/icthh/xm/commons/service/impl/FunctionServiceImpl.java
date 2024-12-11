package com.icthh.xm.commons.service.impl;

import com.icthh.xm.commons.config.FunctionApiSpecConfiguration;
import com.icthh.xm.commons.domain.spec.FunctionSpec;
import com.icthh.xm.commons.permission.service.DynamicPermissionCheckService;
import com.icthh.xm.commons.service.FunctionService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.utils.CollectionsUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.commons.utils.FunctionSpecUtils.filterAndLogByHttpMethod;
import static com.icthh.xm.commons.utils.FunctionSpecUtils.filterByHttpMethod;
import static com.icthh.xm.commons.utils.JsonValidationUtils.assertJson;
import static java.lang.Boolean.TRUE;

@Service
@ConditionalOnMissingBean(FunctionService.class)
public class FunctionServiceImpl extends AbstractFunctionService<FunctionSpec> {

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
    public FunctionSpec findFunctionSpec(String functionKey, String httpMethod) {
        String tenantKey = tenantContextHolder.getTenantKey();
        return functionApiSpecConfiguration.getSpecByKeyAndTenant(functionKey, tenantKey)
            .filter(fs -> filterAndLogByHttpMethod(httpMethod, fs))
            .or(() -> filterByPathAsFunctionKey(tenantKey, functionKey, httpMethod))
            .orElseThrow(() -> new IllegalStateException(
                String.format("Function by key: %s and tenant: %s not found", functionKey, tenantKey)));
    }

    private Optional<FunctionSpec> filterByPathAsFunctionKey(String tenantKey, String functionKey, String httpMethod) {
        return functionApiSpecConfiguration.getOrderedSpecByTenant(tenantKey).stream()
            .filter(fs -> fs.getPath() != null)
            .filter(fs -> matcher.match(fs.getPath(), functionKey))
            .filter(fs -> filterByHttpMethod(httpMethod, fs))
            .findFirst();
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
