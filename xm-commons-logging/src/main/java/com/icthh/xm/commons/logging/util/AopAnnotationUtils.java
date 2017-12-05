package com.icthh.xm.commons.logging.util;

import com.icthh.xm.commons.logging.LoggingAspectConfig;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Utility class for AOP logging.
 */
@UtilityClass
public class AopAnnotationUtils {

    /**
     * Find annotation related to method intercepted by joinPoint.
     *
     * Annotation is searched on method level at first and then if not found on class lever as well.
     *
     * Method uses spring {@link AnnotationUtils} findAnnotation(), so it is search for annotation by class hierarchy.
     *
     * @param joinPoint join point
     * @return Optional value of type @{@link LoggingAspectConfig}
     * @see AnnotationUtils
     */
    public static Optional<LoggingAspectConfig> getConfigAnnotation(JoinPoint joinPoint) {

        Optional<Method> method = getCallingMethod(joinPoint);

        Optional<LoggingAspectConfig> result = method
            .map(m -> AnnotationUtils.findAnnotation(m, LoggingAspectConfig.class));

        if (!result.isPresent()) {
            Optional<Class> clazz = getDeclaringClass(joinPoint);
            result = clazz.map(aClass -> AnnotationUtils.getAnnotation(aClass, LoggingAspectConfig.class));
        }

        return result;
    }

    private static Optional<Method> getCallingMethod(JoinPoint joinPoint) {

        if (joinPoint != null && joinPoint.getSignature() != null) {

            if (joinPoint.getSignature() instanceof MethodSignature) {
                final MethodSignature ms = (MethodSignature) joinPoint.getSignature();
                return Optional.ofNullable(ms.getMethod());
            }

        }
        return Optional.empty();
    }

    private static Optional<Class> getDeclaringClass(JoinPoint joinPoint) {

        if (hasSignature(joinPoint)) {
            return Optional.ofNullable(joinPoint.getSignature().getDeclaringType());
        }
        return Optional.empty();
    }

    private static boolean hasSignature(JoinPoint joinPoint) {
        return joinPoint != null && joinPoint.getSignature() != null;
    }

}
