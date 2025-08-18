package com.icthh.xm.commons.service.impl;

import com.icthh.xm.commons.domain.enums.FunctionFeatureContext;
import com.icthh.xm.commons.domain.spec.IFunctionSpec;
import com.icthh.xm.commons.permission.domain.enums.IFeatureContext;
import com.icthh.xm.commons.permission.service.DynamicPermissionCheckService;
import com.icthh.xm.commons.service.FunctionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import java.util.Map;
import java.util.Objects;

@Slf4j
public abstract class AbstractFunctionService<FS extends IFunctionSpec> implements FunctionService<FS> {

    private final DynamicPermissionCheckService dynamicPermissionCheckService;
    private final AntPathMatcher matcher;

    protected AbstractFunctionService(DynamicPermissionCheckService dynamicPermissionCheckService) {
        this.dynamicPermissionCheckService = dynamicPermissionCheckService;
        this.matcher = new AntPathMatcher();
    }

    @Override
    public void validateFunctionKey(final String functionKey) {
        Objects.requireNonNull(functionKey, "functionKey can't be null");
    }

    @Override
    public void checkPermissions(IFeatureContext featureContext, String basePermission, String functionKey) {
        dynamicPermissionCheckService.checkContextPermission(featureContext, basePermission, functionKey);
    }

    @Override
    public void checkPermissions(String basePermission, String functionKey) {
        checkPermissions(FunctionFeatureContext.FUNCTION, basePermission, functionKey);
    }

    @Override
    public void enrichInputFromPathParams(String functionKey, Map<String, Object> functionInput, FS functionSpec) {
        if (functionSpec.getPath() != null && matcher.match(functionSpec.getPath(), functionKey)) {
            functionInput.putAll(matcher.extractUriTemplateVariables(functionSpec.getPath(), functionKey));
        }
    }

}
