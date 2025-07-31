package com.icthh.xm.commons.lep.impl;

import static com.icthh.xm.commons.lep.impl.MethodEqualsByReferenceWrapper.wrap;
import static com.icthh.xm.commons.lep.spring.LepSpelReader.readFieldByPath;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.TargetProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepExecutor;
import com.icthh.xm.commons.lep.api.LepKey;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.spring.LepContextService;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.lep.api.LepInvocationCauseException;
import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.MethodSignature;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
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

            String group = isNotBlank(lep.group()) ? lep.group() : typeLepService.group();
            LepKey baseLepKey = new DefaultLepKey(group, lep.value());

            MethodSignature methodSignature = buildMethodSignature(targetType, method);
            LepMethod lepMethod = new LepMethodImpl(target, methodSignature, args, baseLepKey);
            LepKey lepKey = resolveLepKey(lep, baseLepKey, lepMethod);

            LepExecutor lepEngine = this.lepEngineService.getLepExecutor(lepKey);
            return lepEngine
                .ifLepPresent(engine -> invokeLepMethod(engine, target, lepMethod, lepKey))
                .ifLepNotExists(() -> invokeOriginalMethod(target, lepMethod, lepKey))
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
        TargetProceedingLep targetProceedingLep = new TargetProceedingLep(target, lepMethod, lepKey);
        BaseLepContext lepContext = buildLepContext(lepEngine, lepMethod, targetProceedingLep);
        return lepEngine.invoke(lepKey, targetProceedingLep, lepContext);
    }

    private BaseLepContext buildLepContext(LepEngine lepEngine, LepMethod lepMethod, TargetProceedingLep targetProceedingLep) {
        String lepContextMethodParameter = lepMethod.getMethodSignature().getLepContextMethodParameter();
        if (lepContextMethodParameter == null) {
            return lepContextService.createLepContext(lepEngine, targetProceedingLep);
        } else {
            BaseLepContext lepContext = lepMethod.getParameter(lepContextMethodParameter, BaseLepContext.class);
            return lepContextService.customize(lepContext, lepEngine, targetProceedingLep);
        }
    }

    @SneakyThrows
    private Object invokeOriginalMethod(Object target, LepMethod lepMethod, LepKey lepKey) {
        Method method = lepMethod.getMethodSignature().getMethod();
        try {
            return method.invoke(target, lepMethod.getMethodArgValues());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Error while processing target method: " + method, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause == null) {
                throw new IllegalStateException("Invocation exception cause is null, "
                    + "while processing target method for LEP resource key: "
                    + lepKey);
            }

            if (cause instanceof Error) {
                throw Error.class.cast(cause);
            } else if (cause instanceof Exception) {
                throw Exception.class.cast(cause);
            } else {
                log.warn("Error execute LEP target method", e);
                throw new IllegalStateException("Error processing target method for LEP resource key: "
                    + lepKey + ". "
                    + cause.getMessage(), cause);
            }
        }
    }

    private LepKey resolveLepKey(LogicExtensionPoint lep, LepKey baseLepKey, LepMethod lepMethod) {
        if (StringUtils.isNoneBlank(lep.resolverExpression())) {
            // Use resolver expression to resolve segments
            return new DefaultLepKey(
                baseLepKey.getGroup(),
                baseLepKey.getBaseKey(),
                resolveSegmentsByExpression(lep.resolverExpression(), lepMethod)
            );
        }

        LepKeyResolver keyResolver = getResolver(lep);
        return new DefaultLepKey(
            keyResolver.group(lepMethod),
            baseLepKey.getBaseKey(),
            keyResolver.segments(lepMethod)
        );
    }

    private List<String> resolveSegmentsByExpression(String expression, LepMethod lepMethod) {
        Map<String, Object> args = lepContextService.buildInArgsMap(lepMethod);
        var segments = readFieldByPath(expression, args);
        if (segments instanceof List<?>) {
            return (List<String>) segments;
        } else if (segments == null) {
            return List.of();
        } else {
            return List.of(String.valueOf(segments));
        }
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
