package com.icthh.xm.commons.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.keyresolver.FunctionLepKeyResolver;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;

import java.util.Map;

/**
 * The {@link FunctionExecutorService} interface.
 */
@IgnoreLogginAspect
@LepService(group = "function")
public interface FunctionExecutorService {

    /**
     * Execute function.
     *
     * @param functionKey   the function key, unique in Tenant
     * @param functionInput function input context
     * @return function result data
     */
    @LogicExtensionPoint(value = "Function", resolver = FunctionLepKeyResolver.class)
    default Map<String, Object> execute(String functionKey, Map<String, Object> functionInput, String httpMethod) {
        throw new IllegalStateException("Function " + functionKey + " not found");
    }

    /**
     * Execute anonymous function.
     *
     * @param functionKey   the function key, unique in Tenant
     * @param functionInput function input context
     * @return function result data
     */
    @LogicExtensionPoint(value = "AnonymousFunction", resolver = FunctionLepKeyResolver.class)
    default Map<String, Object> executeAnonymousFunction(String functionKey, Map<String, Object> functionInput, String httpMethod) {
        throw new IllegalStateException("AnonymousFunction " + functionKey + " not found");
    }
}
