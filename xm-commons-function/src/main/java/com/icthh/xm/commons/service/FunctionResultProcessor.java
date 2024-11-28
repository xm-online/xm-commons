package com.icthh.xm.commons.service;

import com.icthh.xm.commons.domain.FunctionResult;

import java.util.Map;

/**
 * Interface to process executed function result
 *
 * @param <FS> function spec object
 */
public interface FunctionResultProcessor<FS> {

    /**
     * This method processed requested function result
     *
     * @param functionKey    the function key, unique in Tenant
     * @param executorResult function execution result map
     * @param functionSpec   function specification object
     * @return function result
     */
    FunctionResult processFunctionResult(String functionKey, Map<String, Object> executorResult, FS functionSpec);
}
