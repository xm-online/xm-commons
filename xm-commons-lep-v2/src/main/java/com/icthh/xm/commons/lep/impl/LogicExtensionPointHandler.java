package com.icthh.xm.commons.lep.impl;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.lep.api.LepInvocationCauseException;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.MethodSignature;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.icthh.xm.commons.lep.impl.MethodEqualsByReferenceWrapper.wrap;

@Slf4j
@Component
public class LogicExtensionPointHandler {

    private final Map<MethodEqualsByReferenceWrapper, MethodSignature> methodsCache = new ConcurrentHashMap<>();

    @SneakyThrows
    public Object handleLepMethod(Class<?> targetType, Object target, Method method, LogicExtensionPoint lep, Object[] args) {

        MethodSignature methodSignature = buildMethodSignature(targetType, method);
        LepMethod lepMethod = new LepMethodImpl(target, methodSignature, args);


        // get resolver
        // resolve key
        // forEach lep engines isExists
        // buildMethodContext (signature with arguments)
        // runPreprocessors with
        // executeLep

        try {

        } catch (LepInvocationCauseException e) {
            log.debug("Error process target", e);
            throw e.getCause();
        } catch (Exception e) {
            throw e;
        }
        // runPostprocessors
        return null;
    }

    private MethodSignature buildMethodSignature(Class<?> targetType, Method method) {
        return methodsCache.computeIfAbsent(wrap(method), wrapper -> new MethodSignatureImpl(wrapper.getMethod(), targetType));
    }

}
