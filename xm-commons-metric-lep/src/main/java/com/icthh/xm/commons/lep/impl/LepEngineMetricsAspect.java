package com.icthh.xm.commons.lep.impl;

import com.icthh.xm.commons.lep.api.LepEngine;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Aspect
@Component
public class LepEngineMetricsAspect {

    private final LepEngineMetricsDelegate metricsDelegate;

    public LepEngineMetricsAspect(LepEngineMetricsDelegate metricsDelegate) {
        this.metricsDelegate = metricsDelegate;
    }

    @Around(
        "execution(com.icthh.xm.commons.lep.api.LepEngine "
            + "com.icthh.xm.commons.lep.api.LepEngineFactory+.createLepEngine(..))"
    )
    public Object wrapCreatedEngine(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        if (!(result instanceof LepEngine realEngine)) {
            return result;
        }
        return createMetricsProxy(realEngine);
    }

    private LepEngine createMetricsProxy(LepEngine delegate) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(delegate.getClass());
        enhancer.setCallback(new InvokeMetricsInterceptor(delegate, metricsDelegate));
        return (LepEngine) enhancer.create();
    }

    private record InvokeMetricsInterceptor(LepEngine delegate,
                                            LepEngineMetricsDelegate metricsDelegate) implements MethodInterceptor {

        @Override
            public Object intercept(Object self, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                if (!"invoke".equals(method.getName())) {
                    return invokeOnDelegate(method, args);
                }

                Object lepKey = (args != null && args.length > 0) ? args[0] : null;
                String engineId = delegate.getId();

                return metricsDelegate.recordLepExecutionMetrics(delegate, lepKey, engineId,
                        () -> invokeOnDelegate(method, args));
            }

            private Object invokeOnDelegate(Method method, Object[] args) throws Throwable {
                try {
                    return method.invoke(delegate, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }
}
