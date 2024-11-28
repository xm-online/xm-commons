package com.icthh.xm.commons.service.impl;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.keyresolver.FunctionLepKeyResolver;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.service.FunctionExecutorService;

import java.util.Map;

@IgnoreLogginAspect
@LepService(group = "function")
public class FunctionExecutorServiceImpl implements FunctionExecutorService {

    /**
     * Execute function.
     *
     * @param functionKey   the function key, unique in Tenant
     * @param functionInput function input context
     * @return function result data
     */
    @Override
    @LogicExtensionPoint(value = "Function", resolver = FunctionLepKeyResolver.class)
    public Map<String, Object> execute(String functionKey, Map<String, Object> functionInput, String httpMethod) {
        throw new IllegalStateException("Function " + functionKey + " not found");
    }

    /**
     * Execute anonymous function.
     *
     * @param functionKey   the function key, unique in Tenant
     * @param functionInput function input context
     * @return function result data
     */
    @Override
    @LogicExtensionPoint(value = "AnonymousFunction", resolver = FunctionLepKeyResolver.class)
    public Map<String, Object> executeAnonymousFunction(String functionKey, Map<String, Object> functionInput, String httpMethod) {
        throw new IllegalStateException("AnonymousFunction " + functionKey + " not found");
    }
}
