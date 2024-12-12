package com.icthh.xm.commons.service;

import com.icthh.xm.commons.domain.FunctionResult;
import com.icthh.xm.commons.domain.spec.IFunctionSpec;

import static java.lang.Boolean.FALSE;

/**
 * Interface to process executed function result
 *
 * @param <FS> function spec object
 */
public interface FunctionResultProcessor<FS extends IFunctionSpec> {

    /**
     * This method processed requested function result
     *
     * @param functionKey    the function key, unique in Tenant
     * @param executorResult function execution result map
     * @param functionSpec   function specification object
     * @return function result
     */
    default FunctionResult processFunctionResult(String functionKey, Object executorResult, FS functionSpec) {
        return FALSE.equals(functionSpec.getWrapResult()) && executorResult instanceof FunctionResult
            ? (FunctionResult) executorResult
            : wrapFunctionResult(functionKey, executorResult, functionSpec);
    }

    /**
     * This method processed requested function result
     *
     * @param functionKey    the function key, unique in Tenant
     * @param executorResult function execution result map
     * @param functionSpec   function specification object
     * @return function result
     */
    FunctionResult wrapFunctionResult(String functionKey, Object executorResult, FS functionSpec);
}
