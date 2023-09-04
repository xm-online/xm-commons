package com.icthh.xm.commons.lep.impl;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;

@Aspect
public class LepMethodAspect {

    public LepMethodAspect() {
        super();
    }

    /**
     * LEP service handler.
     */
    @Autowired
    private LogicExtensionPointHandler lepHandler;

    @Around("@annotation(com.icthh.xm.commons.lep.LogicExtensionPoint)")
    @SuppressWarnings("squid:S00112")
    // suppress throwable waring, this method cat throw any throwable
    public Object logicExtensionPoint(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> declaringType = signature.getDeclaringType();

        if (!declaringType.isAnnotationPresent(LepService.class)) {
            throw new IllegalStateException("Bean class " + declaringType + " has no LepService annotation, "
                                                + "but have LogicExtensionPoint annotation on method: "
                                                + signature);
        }

        Method method = signature.getMethod();
        LogicExtensionPoint lep = method.getAnnotation(LogicExtensionPoint.class);
        Object resultValue;
        if (lep == null) {
            resultValue = joinPoint.proceed();
        } else {
            resultValue = lepHandler.handleLepMethod(declaringType, joinPoint.getTarget(), method, lep, joinPoint.getArgs());
        }
        return resultValue;
    }

}
