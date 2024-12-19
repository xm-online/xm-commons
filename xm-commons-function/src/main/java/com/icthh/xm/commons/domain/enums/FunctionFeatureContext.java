package com.icthh.xm.commons.domain.enums;

import com.icthh.xm.commons.permission.domain.enums.IFeatureContext;
import com.icthh.xm.commons.permission.service.DynamicPermissionCheckService;
import com.icthh.xm.commons.service.impl.DynamicPermissionCheckServiceImpl;
import lombok.AllArgsConstructor;

import java.util.function.Function;

@AllArgsConstructor
public enum FunctionFeatureContext implements IFeatureContext {

    FUNCTION(DynamicPermissionCheckServiceImpl::isDynamicFunctionPermissionEnabled);

    private final Function<DynamicPermissionCheckServiceImpl, Boolean> featureContextResolver;

    @Override
    public boolean isEnabled(DynamicPermissionCheckService service) {
        if (service instanceof DynamicPermissionCheckServiceImpl) {
            return this.featureContextResolver.apply((DynamicPermissionCheckServiceImpl) service);
        }
        throw new IllegalArgumentException("Invalid check service type: " + service.getClass().getName());
    }
}
