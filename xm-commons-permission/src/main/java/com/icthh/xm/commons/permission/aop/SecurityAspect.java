package com.icthh.xm.commons.permission.aop;

import org.aspectj.lang.annotation.Aspect;

@Aspect
public class SecurityAspect {

//    @Pointcut("@annotation(org.springframework.security.access.prepost.PreAuthorize) " +
//        "|| @annotation(org.springframework.security.access.prepost.PostAuthorize) "
//        + "|| within(@org.springframework.security.access.prepost.PreAuthorize *) " +
//        " || within(@org.springframework.security.access.prepost.PostAuthorize *)")
//    public void excluded() {
//    }
//
//    @Pointcut("execution(* com.icthh.xm..web.rest..*(..))")
//    public void restPointcut() {
//    }
//
//    @Before("restPointcut() && !excluded()")
//    public Object beforeRest(JoinPoint joinPoint) {
//
//        throw new AccessDeniedException("Pre- or PostAuthorize not specified");
//
//    }
}
