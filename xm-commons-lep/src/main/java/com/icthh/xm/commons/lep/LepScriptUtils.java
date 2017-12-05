package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.ContextScopes;
import com.icthh.xm.lep.api.LepInvocationCauseException;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.ScopedContext;
import com.icthh.xm.lep.api.commons.UrlLepResourceKey;
import com.icthh.xm.lep.groovy.GroovyScriptRunner;
import groovy.lang.Binding;
import lombok.experimental.UtilityClass;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The {@link LepScriptUtils} class.
 */
@UtilityClass
@SuppressWarnings("squid:S1118") // private constructor generated by lombok
final class LepScriptUtils {

    /**
     * Executes any script.
     *
     * @param scriptResourceKey        current script resource key
     * @param proceedingLep            method proceed for Around scripts
     * @param method                   LEP method
     * @param managerService           LEP manager service
     * @param resourceExecutorSupplier LEP resource script executor supplier
     * @param methodResult             LEP method result
     * @param overrodeArgValues        arg values to override (can be {@code null})
     * @return Groovy script binding
     * @throws LepInvocationCauseException when exception in script occurs
     */
    static Object executeScript(UrlLepResourceKey scriptResourceKey,
                                ProceedingLep proceedingLep, // can be null
                                LepMethod method,
                                LepManagerService managerService,
                                Supplier<GroovyScriptRunner> resourceExecutorSupplier,
                                LepMethodResult methodResult, // can be null
                                Object... overrodeArgValues) throws LepInvocationCauseException {

        GroovyScriptRunner runner = resourceExecutorSupplier.get();
        String scriptName = runner.getResourceKeyMapper().map(scriptResourceKey);

        Binding binding = buildBinding(scriptResourceKey, managerService, method, proceedingLep, methodResult,
                                       overrodeArgValues);

        return runner.runScript(scriptResourceKey, method, managerService, scriptName, binding);

    }

    /**
     * Build scripts bindings.
     *
     * @param scriptResourceKey current script resource key
     * @param managerService    LEP manager service
     * @param method            LEP method
     * @param proceedingLep     proceeding object (can be {@code null})
     * @param overrodeArgValues arg values to override (can be {@code null})
     * @return Groovy script binding
     */
    private static Binding buildBinding(UrlLepResourceKey scriptResourceKey,
                                        LepManagerService managerService,
                                        LepMethod method,
                                        ProceedingLep proceedingLep,
                                        LepMethodResult lepMethodResult,
                                        Object... overrodeArgValues) {
        boolean isOverrodeArgs = overrodeArgValues != null && overrodeArgValues.length > 0;
        if (isOverrodeArgs) {
            int actual = overrodeArgValues.length;
            int expected = method.getMethodSignature().getParameterTypes().length;
            if (actual != expected) {
                throw new IllegalArgumentException("When calling LEP resource: " + scriptResourceKey
                                                   + ", overrode method argument values "
                                                   + "count doesn't corresponds method signature (expected: "
                                                   + expected + ", actual: " + actual + ")");
            }
        }

        Map<String, Object> lepContext = new LinkedHashMap<>();
        Binding binding = new Binding();

        // add execution context values
        ScopedContext executionContext = managerService.getContext(ContextScopes.EXECUTION);
        if (executionContext != null) {
            executionContext.getValues().forEach(lepContext::put);
        }

        // add method arg values
        final String[] parameterNames = method.getMethodSignature().getParameterNames();
        final Object[] methodArgValues = isOverrodeArgs ? overrodeArgValues : method.getMethodArgValues();
        Map<String, Object> inVars = new LinkedHashMap<>(parameterNames.length);
        for (int i = 0; i < parameterNames.length; i++) {
            String paramName = parameterNames[i];
            Object paramValue = methodArgValues[i];

            inVars.put(paramName, paramValue);
        }
        lepContext.put(XmLepScriptConstants.BINDING_KEY_IN_ARGS, inVars);

        // add proceedingLep support
        lepContext.put(XmLepScriptConstants.BINDING_KEY_LEP, proceedingLep);

        // add returned value
        if (lepMethodResult != null) {
            lepContext.put(XmLepScriptConstants.BINDING_KEY_RETURNED_VALUE, lepMethodResult.getReturnedValue());
        }

        // add method result
        lepContext.put(XmLepScriptConstants.BINDING_KEY_METHOD_RESULT, lepMethodResult);

        binding.setVariable(XmLepScriptConstants.BINDING_VAR_LEP_SCRIPT_CONTEXT, lepContext);
        return binding;
    }

}
