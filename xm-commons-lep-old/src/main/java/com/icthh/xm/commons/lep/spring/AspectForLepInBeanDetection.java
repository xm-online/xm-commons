package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * The {@link AspectForLepInBeanDetection} class.
 */
@Aspect
@Component
public class AspectForLepInBeanDetection {

    public AspectForLepInBeanDetection() {
        super();
    }

    /**
     * LEP service handler.
     */
    @Autowired
    private LepServiceHandler lepServiceHandler;

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
        LepService lepService = declaringType.getAnnotation(LepService.class);
        LogicExtensionPoint lep = method.getAnnotation(LogicExtensionPoint.class);

        Object resultValue;
        if (lep == null && lepService.explicitMethods()) {
            resultValue = joinPoint.proceed();
        } else {
            resultValue = lepServiceHandler.onMethodInvoke(declaringType, joinPoint.getTarget(),
                                                           method, joinPoint.getArgs());
        }

        return resultValue;
    }

}
