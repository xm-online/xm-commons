package com.icthh.xm.commons.service;

import com.icthh.xm.commons.domain.spec.IFunctionSpec;
import com.icthh.xm.commons.permission.domain.enums.IFeatureContext;

import java.util.Map;

/**
 * Interface for function`s related operations
 * @param <FS> function spec object
 */
public interface FunctionService<FS extends IFunctionSpec> {

    void validateFunctionKey(final String functionKey);

    void checkPermissions(String basePermission, String functionKey);

    void checkPermissions(IFeatureContext featureContext, String basePermission, String functionKey);

    FS findFunctionSpec(String functionKey, String httpMethod);

    Map<String, Object> getValidFunctionInput(FS functionSpec, Map<String, Object> functionInput);

    void enrichInputFromPathParams(String functionKey, Map<String, Object> functionInput, FS functionSpec);

}
