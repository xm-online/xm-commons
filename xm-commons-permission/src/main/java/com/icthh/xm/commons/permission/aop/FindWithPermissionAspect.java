package com.icthh.xm.commons.permission.aop;

import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class FindWithPermissionAspect {

    /**
     * Transfer privilege key from annotation to method invocation.
     * @param joinPoint the join point
     * @param annotation the annotation with privilege key
     * @return method invocation result
     */
    @SneakyThrows
    @Around("@annotation(annotation)")
    public Object transferPrivilege(ProceedingJoinPoint joinPoint, FindWithPermission annotation) {
        Object[] args = joinPoint.getArgs();
        args[args.length - 1] = annotation.value();

        return joinPoint.proceed(args);
    }
}
