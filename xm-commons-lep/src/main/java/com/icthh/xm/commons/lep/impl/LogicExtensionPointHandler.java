package com.icthh.xm.commons.lep.impl;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.TargetProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.api.LepExecutor;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepKey;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.lep.api.LepInvocationCauseException;
import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.MethodSignature;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.icthh.xm.commons.lep.impl.MethodEqualsByReferenceWrapper.wrap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

@Slf4j
@Component
public class LogicExtensionPointHandler {

    private final Map<MethodEqualsByReferenceWrapper, MethodSignature> methodsCache = new ConcurrentHashMap<>();
    private final Map<Class<? extends LepKeyResolver>, LepKeyResolver> resolvers;
    private final LepManagementService lepEngineService;
    private final LepContextService lepContextService;

    public LogicExtensionPointHandler(List<LepKeyResolver> resolverList, LepManagementService lepEngineService, LepContextService lepContextService) {
        Map<Class<? extends LepKeyResolver>, LepKeyResolver> resolvers = new HashMap<>();
        resolverList.forEach(it -> resolvers.put(it.getClass(), it));
        this.resolvers = unmodifiableMap(resolvers);
        this.lepEngineService = lepEngineService;
        this.lepContextService = lepContextService;
    }

    @SneakyThrows
    public Object handleLepMethod(Class<?> targetType, Object target, Method method, LogicExtensionPoint lep, Object[] args) {
        try {
            LepService typeLepService = targetType.getAnnotation(LepService.class);
            requireNonNull(typeLepService, () -> "No " + LepService.class.getSimpleName()
                + " annotation for type " + targetType.getCanonicalName());

            String group = requireNonNullElse(lep.group(), typeLepService.group());
            LepKey baseLepKey = new DefaultLepKey(group, lep.value());

            MethodSignature methodSignature = buildMethodSignature(targetType, method);
            LepMethod lepMethod = new LepMethodImpl(target, methodSignature, args, baseLepKey);
            LepKey lepKey = resolveLepKey(lep, baseLepKey, lepMethod);

            LepExecutor lepEngine = this.lepEngineService.getLepExecutor(lepKey);
            return lepEngine
                .ifLepPresent(engine -> invokeLepMethod(engine, target, lepMethod, lepKey))
                .ifLepNotExists(() -> invokeOriginalMethod(target, lepMethod))
                .getMethodResult();

        } catch (LepInvocationCauseException e) {
            log.error("Error process lep", e);
            throw e.getCause();
        } catch (Throwable e) { // Throwable to catch groovy errors
            log.error("Error process lep", e);
            throw e;
        }
    }

    private Object invokeLepMethod(LepEngine lepEngine, Object target, LepMethod lepMethod, LepKey lepKey) {
        // TODO start logs see LoggingConfigService
        TargetProceedingLep targetProceedingLep = new TargetProceedingLep(target, lepMethod, lepKey);
        BaseLepContext lepContext = lepContextService.createLepContext(targetProceedingLep);
        Object result = lepEngine.invoke(lepKey, targetProceedingLep, lepContext);
        // TODO end logs see LoggingConfigService
        return result;
    }

    @SneakyThrows
    private Object invokeOriginalMethod(Object target, LepMethod lepMethod){
        return lepMethod.getMethodSignature().getMethod().invoke(target, lepMethod.getMethodArgValues());
    }

    private LepKey resolveLepKey(LogicExtensionPoint lep, LepKey baseLepKey, LepMethod lepMethod) {
        LepKeyResolver keyResolver = getResolver(lep);
        return new DefaultLepKey(
            keyResolver.group(lepMethod),
            baseLepKey.getBaseKey(),
            keyResolver.segments(lepMethod)
        );
    }

    private LepKeyResolver getResolver(LogicExtensionPoint lep) {
        LepKeyResolver lepKeyResolver = this.resolvers.get(lep.resolver());
        requireNonNull(lepKeyResolver, () -> "Lep key resolver " + lep.resolver().getCanonicalName() + " must be spring bean");
        return lepKeyResolver;
    }

    private MethodSignature buildMethodSignature(Class<?> targetType, Method method) {
        return methodsCache.computeIfAbsent(wrap(method), wrapper -> new MethodSignatureImpl(wrapper.getMethod(), targetType));
    }

}
