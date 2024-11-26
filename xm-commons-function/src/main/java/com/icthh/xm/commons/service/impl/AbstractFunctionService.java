package com.icthh.xm.commons.service.impl;

import com.icthh.xm.commons.domain.enums.FunctionFeatureContext;
import com.icthh.xm.commons.permission.domain.enums.IFeatureContext;
import com.icthh.xm.commons.permission.service.DynamicPermissionCheckService;
import com.icthh.xm.commons.service.FunctionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
@AllArgsConstructor
public abstract class AbstractFunctionService<FS> implements FunctionService<FS> {

    private final DynamicPermissionCheckService dynamicPermissionCheckService;

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
}
