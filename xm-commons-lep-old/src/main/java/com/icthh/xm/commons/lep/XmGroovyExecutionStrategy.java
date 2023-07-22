package com.icthh.xm.commons.lep;

import static com.icthh.xm.commons.lep.XmLepConstants.SCRIPT_NAME_SEPARATOR;
import static com.icthh.xm.commons.lep.XmLepConstants.URL_DELIMITER;
import static com.icthh.xm.commons.lep.XmLepConstants.URL_NET_PATH_DELIMITER;
import static com.icthh.xm.commons.lep.XmLepResourceSubType.AFTER;
import static com.icthh.xm.commons.lep.XmLepResourceSubType.AROUND;
import static com.icthh.xm.commons.lep.XmLepResourceSubType.BEFORE;
import static com.icthh.xm.commons.lep.XmLepResourceSubType.DEFAULT;
import static com.icthh.xm.commons.lep.XmLepResourceSubType.TENANT;

import com.icthh.xm.lep.api.LepInvocationCauseException;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.UrlLepResourceKey;
import com.icthh.xm.lep.groovy.GroovyExecutionStrategy;
import com.icthh.xm.lep.groovy.GroovyScriptRunner;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The {@link XmGroovyExecutionStrategy} class.
 */
@Slf4j
public class XmGroovyExecutionStrategy implements GroovyExecutionStrategy {

    @Override
    public Object executeLepResource(UrlLepResourceKey compositeResourceKey,
                                     LepMethod method,
                                     LepManagerService managerService,
                                     Supplier<GroovyScriptRunner> resourceExecutorSupplier) throws LepInvocationCauseException {

        Map<XmLepResourceSubType, UrlLepResourceKey> atomicResourceKeys = getAvailableAtomicResourceKeys(
            compositeResourceKey, managerService);

        // validate resource sub type combinations
        XmLepScriptRules.validateScriptsCombination(atomicResourceKeys.keySet(),
                                                    method,
                                                    compositeResourceKey);

        if (atomicResourceKeys.isEmpty()) {
            // target method execution case
            return onNoScripts(compositeResourceKey, method);
        } else if (atomicResourceKeys.containsKey(AROUND)) {
            // AROUND script call case
            return onAroundScript(compositeResourceKey, atomicResourceKeys, method, managerService,
                                  resourceExecutorSupplier);
        } else {
            // BEFORE and/or TENANT and/or DEFAULT and/or AFTER script call case
            return onWithoutAroundScript(compositeResourceKey, atomicResourceKeys, method,
                                         managerService, resourceExecutorSupplier);
        }
    }

    private static Object onNoScripts(UrlLepResourceKey compositeResourceKey, LepMethod method) throws LepInvocationCauseException {
        Object target = method.getTarget();
        if (target == null) {
            throw new IllegalStateException(String.format(
                "LEP resource key %s has no defined script(s) and target object. "
                    + "Possible LEP defined in interface and has no default script provided.",
                compositeResourceKey));
        }

        return executeLepTargetMethod(compositeResourceKey, method, target).processResult();
    }

    private static MethodResultProcessor executeLepTargetMethod(UrlLepResourceKey compositeResourceKey,
                                                                LepMethod method,
                                                                Object target) throws LepInvocationCauseException {
        Method jvmMethod = method.getMethodSignature().getMethod();
        try {
            return MethodResultProcessor.valueOf(jvmMethod.invoke(target, method.getMethodArgValues()));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Error invoking target method for LEP resource key: "
                                                + compositeResourceKey + ". " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            log.info("Exception while invoking target method for LEP resource key: {}", compositeResourceKey, e);

            Throwable cause = e.getCause();
            if (cause == null) {
                throw new IllegalStateException("Null cause while invoking target method for LEP resource key: "
                                                    + compositeResourceKey);
            }

            LepInvocationCauseException methodException;
            if (cause instanceof Error) {
                throw Error.class.cast(cause);
            } else if (cause instanceof Exception) {
                methodException = new LepInvocationCauseException((Exception) cause);
            } else {
                IllegalStateException ex = new IllegalStateException("Error invoking target method for LEP resource key: "
                                                                         + compositeResourceKey + ". "
                                                                         + cause.getMessage(), cause);
                methodException = new LepInvocationCauseException(ex);
            }

            return MethodResultProcessor.valueOf(methodException);
        }
    }

    /**
     * Processing AROUND script.
     * <p>
     * Possible cases (AROUND + TENANT not allowed in current implementation):
     * <ol>
     * <li>AROUND</li>
     * <li>AROUND + DEFAULT</li>
     * </ol>
     * .
     *
     * @param compositeResourceKey     resource key that corresponds to extension key of current LEP
     *                                 (contains AROUND key)
     * @param atomicResourceKeys       script resource keys corresponds to composite resource key
     * @param method                   current processed LEP method
     * @param managerService           LEP manager service
     * @param resourceExecutorSupplier LEP resource executor supplier
     * @return LEP method result object
     */
    private Object onAroundScript(UrlLepResourceKey compositeResourceKey,
                                  Map<XmLepResourceSubType, UrlLepResourceKey> atomicResourceKeys,
                                  LepMethod method,
                                  LepManagerService managerService,
                                  Supplier<GroovyScriptRunner> resourceExecutorSupplier)
        throws LepInvocationCauseException {

        UrlLepResourceKey aroundKey = atomicResourceKeys.get(AROUND);
        UrlLepResourceKey defaultKey = atomicResourceKeys.get(DEFAULT);
        ProceedingLep proceedingLep = buildProceedingLep(compositeResourceKey, defaultKey, method,
                                                         managerService, resourceExecutorSupplier);

        return LepScriptUtils.executeScript(aroundKey, proceedingLep, method,
                                            managerService, resourceExecutorSupplier, null);
    }

    // BEFORE and/or TENANT and/or DEFAULT and/or AFTER script call case
    private Object onWithoutAroundScript(UrlLepResourceKey compositeResourceKey,
                                         Map<XmLepResourceSubType, UrlLepResourceKey> atomicResourceKeys,
                                         LepMethod method,
                                         LepManagerService managerService,
                                         Supplier<GroovyScriptRunner> resourceExecutorSupplier)
        throws LepInvocationCauseException {

        Object target = method.getTarget();

        // Call BEFORE script if it exists
        executeBeforeIfExists(atomicResourceKeys, method, managerService, resourceExecutorSupplier);

        MethodResultProcessor methodResultProcessor;

        if (atomicResourceKeys.containsKey(TENANT)) {
            // Call TENANT script if it exists
            methodResultProcessor = executeLepScript(atomicResourceKeys, method, managerService, resourceExecutorSupplier,
                                                     TENANT);
        } else if (atomicResourceKeys.containsKey(DEFAULT)) {
            // Call DEFAULT script if it exists
            methodResultProcessor = executeLepScript(atomicResourceKeys, method, managerService, resourceExecutorSupplier,
                                                     DEFAULT);
        } else {
            // Call method on target object
            methodResultProcessor = executeLepTargetMethod(compositeResourceKey, method, target);
        }

        // Call AFTER script if it exists
        methodResultProcessor = executeAfterScriptIfExists(atomicResourceKeys,
                                                           method,
                                                           managerService,
                                                           resourceExecutorSupplier,
                                                           methodResultProcessor);

        return methodResultProcessor.processResult();
    }

    private void executeBeforeIfExists(Map<XmLepResourceSubType, UrlLepResourceKey> atomicResourceKeys,
                                       LepMethod method,
                                       LepManagerService managerService,
                                       Supplier<GroovyScriptRunner> resourceExecutorSupplier) throws LepInvocationCauseException {
        if (atomicResourceKeys.containsKey(BEFORE)) {
            UrlLepResourceKey beforeKey = atomicResourceKeys.get(BEFORE);
            // FIXME copy args for method BeanUtils.copyProperties();
            LepScriptUtils.executeScript(beforeKey, null, method, managerService,
                                         resourceExecutorSupplier, null);

        }
    }

    private static MethodResultProcessor executeAfterScriptIfExists(Map<XmLepResourceSubType, UrlLepResourceKey> atomicResourceKeys,
                                                                    LepMethod method,
                                                                    LepManagerService managerService,
                                                                    Supplier<GroovyScriptRunner> resourceExecutorSupplier,
                                                                    MethodResultProcessor methodResultProcessor) {
        if (!atomicResourceKeys.containsKey(AFTER)) {
            return methodResultProcessor;
        }

        // now supporting only AfterRunning advice like scripts
        if (methodResultProcessor.isError()) {
            return methodResultProcessor;
        }

        UrlLepResourceKey afterKey = atomicResourceKeys.get(AFTER);
        try {
            LepScriptUtils.executeScript(afterKey,
                                         null,
                                         method,
                                         managerService,
                                         resourceExecutorSupplier,
                                         LepMethodResult.valueOf(methodResultProcessor));
        } catch (LepInvocationCauseException e) {
            return MethodResultProcessor.valueOf(e); // FIXME what if methodException != null
        }

        return methodResultProcessor;
    }

    private static MethodResultProcessor executeLepScript(Map<XmLepResourceSubType, UrlLepResourceKey> atomicResourceKeys,
                                                          LepMethod method, LepManagerService managerService,
                                                          Supplier<GroovyScriptRunner> resourceExecutorSupplier,
                                                          XmLepResourceSubType key) {
        try {
            Object value = LepScriptUtils.executeScript(atomicResourceKeys.get(key), null, method,
                                                        managerService, resourceExecutorSupplier, null);
            return MethodResultProcessor.valueOf(value);
        } catch (LepInvocationCauseException e) {
            return MethodResultProcessor.valueOf(e);
        }
    }

    private static ProceedingLep buildProceedingLep(UrlLepResourceKey compositeResourceKey,
                                                    UrlLepResourceKey defaultKey,
                                                    LepMethod lepMethod,
                                                    LepManagerService managerService,
                                                    Supplier<GroovyScriptRunner> resourceExecutorSupplier) {
        if (defaultKey != null) {
            return new ScriptLepResourceProceedingLep(compositeResourceKey, defaultKey, lepMethod,
                                                      managerService, resourceExecutorSupplier);
        } else if (lepMethod.getTarget() != null) {
            return new TargetProceedingLep(lepMethod, compositeResourceKey);
        } else {
            throw new IllegalArgumentException("Resource key " + compositeResourceKey
                                                   + " has defaultKey and target object both null, what is inadmissible");
        }
    }

    // Composite key:
    // lep://<tenant>/some/group/<script_name>$<entityType>$<from_state_name>$<to_state_name>.groovy
    // lep://xm/com/icthh/lep/<script_name>$<entityType>$<state>.groovy
    //
    // Atomic key:
    // lep://<tenant>some/group/<script_name>$<entityType>$<from_state_name>$<to_state_name>$<script_type>.groovy
    // lep://xm/com/icthh/lep/<script_name>$<entityType>$<state>$<script_type>.groovy
    public Map<XmLepResourceSubType, UrlLepResourceKey> getAvailableAtomicResourceKeys(
        UrlLepResourceKey compositeResourceKey,
        LepManagerService managerService) {

        // add '/' at start if not exists
        String compositePath = compositeResourceKey.getUrlResourcePath();
        if (!compositePath.startsWith(URL_DELIMITER)) {
            compositePath = URL_DELIMITER + compositePath;
        }

        // get all path without script file extension
        int extIndex = compositePath.lastIndexOf(XmLepConstants.SCRIPT_EXTENSION_SEPARATOR);
        if (extIndex <= 0) {
            throw new IllegalArgumentException(
                "LEP resource name must ends with *.<extension>, actual value: "
                    + compositePath);
        }
        final String scriptBasePath = compositePath.substring(0, extIndex);
        final String extension = compositePath.substring(extIndex);

        String tenantKey = LepContextUtils.getTenantKey(managerService);

        Map<XmLepResourceSubType, UrlLepResourceKey> resourceKeyMap = new EnumMap<>(XmLepResourceSubType.class);
        for (XmLepResourceSubType type : XmLepResourceSubType.values()) {
            // build atomic key for script type
            String resourceUrlPath = URL_NET_PATH_DELIMITER + tenantKey + scriptBasePath +
                                     SCRIPT_NAME_SEPARATOR + type.getName() + extension;
            UrlLepResourceKey atomicKey = UrlLepResourceKey.valueOfUrlResourcePath(resourceUrlPath);

            // if resource for key exists add it to map
            if (managerService.getResourceService().isResourceExists(managerService, atomicKey)) {
                resourceKeyMap.put(type, atomicKey);
            }
        }

        return resourceKeyMap;
    }

}
