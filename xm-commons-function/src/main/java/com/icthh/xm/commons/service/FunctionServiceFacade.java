package com.icthh.xm.commons.service;

import com.icthh.xm.commons.domain.FunctionResult;

import java.util.Map;

/**
 * Interface for functions managing.
 */
public interface FunctionServiceFacade {

    /**
     * Execute function.
     *
     * @param functionKey   the function key, unique in Tenant
     * @param functionInput function input context
     * @param httpMethod    HTTP method used to invoke the function
     * @return function execution result
     */
    FunctionResult execute(String functionKey, Map<String, Object> functionInput, String httpMethod);

     /**
     * Execute anonymous function.
     *
     * @param functionKey   the function key, unique in Tenant
     * @param functionInput function input context
     * @param httpMethod    HTTP method used to invoke the function
     * @return function execution result
     */
     FunctionResult executeAnonymous(String functionKey, Map<String, Object> functionInput, String httpMethod);

}
