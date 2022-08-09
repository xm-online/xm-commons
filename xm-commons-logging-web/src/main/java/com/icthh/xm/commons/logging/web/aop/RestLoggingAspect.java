package com.icthh.xm.commons.logging.web.aop;

import com.icthh.xm.commons.logging.config.LoggingConfig.LogConfiguration;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.util.BasePackageDetector;
import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.logging.web.util.WebLogObjectPrinter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Arrays;
import java.util.Objects;

import static com.icthh.xm.commons.logging.config.LoggingConfig.DEFAULT_LOG_RESULT_DETAILS;
import static com.icthh.xm.commons.logging.util.LogObjectPrinter.Level.OFF_LOG;
import static com.icthh.xm.commons.logging.util.LogObjectPrinter.logWithLevel;


/**
 * Aspect for REST controller logging.
 * Created by medved on 26.06.17.
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class RestLoggingAspect {

    private static final String XM_BASE_PACKAGE = "com.icthh.xm";
    private static final String LOG_START_PATTERN = "START {} : {} --> {}, input: {}";
    private static final String LOG_STOP_PATTERN = "STOP  {} : {} --> {}, result: {}, time = {} ms";
    private static final String LOG_ERROR_PATTERN = "STOP  {} : {} --> {}, error: {}, time = {} ms";

    private final LoggingConfigService loggingConfigService;
    private final BasePackageDetector basePackageDetector;

    @Value("${base-package:'-'}")
    private String basePackage;

    private boolean withLogging(String className) {
        String detectedBasePackage = basePackageDetector.getBasePackage();
        return className.startsWith(XM_BASE_PACKAGE) || className.startsWith(basePackage) || className.startsWith(detectedBasePackage);
    }

    @SuppressWarnings("squid:S1186") //suppress enpty method warning
    @Pointcut("@target(controllerMapping) && @annotation(methodMapping)")
    public void restControllerGetPointcut(RequestMapping controllerMapping, GetMapping methodMapping) {
    }

    @SuppressWarnings("squid:S1186") //suppress enpty method warning
    @Pointcut("@target(controllerMapping) && @annotation(methodMapping)")
    public void restControllerPostPointcut(RequestMapping controllerMapping, PostMapping methodMapping) {
    }

    @SuppressWarnings("squid:S1186") //suppress enpty method warning
    @Pointcut("@target(controllerMapping) && @annotation(methodMapping)")
    public void restControllerPutPointcut(RequestMapping controllerMapping, PutMapping methodMapping) {
    }

    @SuppressWarnings("squid:S1186") //suppress enpty method warning
    @Pointcut("@target(controllerMapping) && @annotation(methodMapping)")
    public void restControllerDeletePointcut(RequestMapping controllerMapping, DeleteMapping methodMapping) {
    }

    @Before("restControllerGetPointcut(controllerMapping, methodMapping)")
    public void logBeforeRest(JoinPoint joinPoint, RequestMapping controllerMapping, GetMapping methodMapping) {
        logStart(joinPoint, HttpMethod.GET.name(), controllerMapping.value(), methodMapping.value());
    }

    @Before("restControllerPostPointcut(controllerMapping, methodMapping)")
    public void logBeforeRest(JoinPoint joinPoint, RequestMapping controllerMapping, PostMapping methodMapping) {
        logStart(joinPoint, HttpMethod.POST.name(), controllerMapping.value(), methodMapping.value());
    }

    @Before("restControllerPutPointcut(controllerMapping, methodMapping)")
    public void logBeforeRest(JoinPoint joinPoint, RequestMapping controllerMapping, PutMapping methodMapping) {
        logStart(joinPoint, HttpMethod.PUT.name(), controllerMapping.value(), methodMapping.value());
    }

    @Before("restControllerDeletePointcut(controllerMapping, methodMapping)")
    public void logBeforeRest(JoinPoint joinPoint, RequestMapping controllerMapping, DeleteMapping
        methodMapping) {
        logStart(joinPoint, HttpMethod.DELETE.name(), controllerMapping.value(), methodMapping.value());
    }

    @AfterReturning(value = "restControllerGetPointcut(controllerMapping, methodMapping)", returning = "result")
    public void logAfterRest(JoinPoint joinPoint, RequestMapping controllerMapping, GetMapping methodMapping,
                             Object result) {
        logStop(joinPoint, HttpMethod.GET.name(), controllerMapping.value(), methodMapping.value(), result);
    }

    @AfterReturning(value = "restControllerPostPointcut(controllerMapping, methodMapping)", returning = "result")
    public void logAfterRest(JoinPoint joinPoint, RequestMapping controllerMapping, PostMapping methodMapping,
                             Object result) {
        logStop(joinPoint, HttpMethod.POST.name(), controllerMapping.value(), methodMapping.value(), result);
    }

    @AfterReturning(value = "restControllerPutPointcut(controllerMapping, methodMapping)", returning = "result")
    public void logAfterRest(JoinPoint joinPoint, RequestMapping controllerMapping, PutMapping methodMapping,
                             Object result) {
        logStop(joinPoint, HttpMethod.PUT.name(), controllerMapping.value(), methodMapping.value(), result);
    }

    @AfterReturning(value = "restControllerDeletePointcut(controllerMapping, methodMapping)", returning = "result")
    public void logAfterRest(JoinPoint joinPoint, RequestMapping controllerMapping, DeleteMapping methodMapping,
                             Object result) {
        logStop(joinPoint, HttpMethod.DELETE.name(), controllerMapping.value(), methodMapping.value(), result);
    }

    @AfterThrowing(value = "restControllerGetPointcut(controllerMapping, methodMapping)", throwing = "e")
    public void logAfterRestThrowing(JoinPoint joinPoint, RequestMapping controllerMapping, GetMapping methodMapping,
                                     Throwable e) {
        logError(joinPoint, HttpMethod.GET.name(), controllerMapping.value(), methodMapping.value(), e);
    }

    @AfterThrowing(value = "restControllerPostPointcut(controllerMapping, methodMapping)", throwing = "e")
    public void logAfterRestThrowing(JoinPoint joinPoint, RequestMapping controllerMapping, PostMapping methodMapping,
                                     Throwable e) {
        logError(joinPoint, HttpMethod.POST.name(), controllerMapping.value(), methodMapping.value(), e);
    }

    @AfterThrowing(value = "restControllerPutPointcut(controllerMapping, methodMapping)", throwing = "e")
    public void logAfterRestThrowing(JoinPoint joinPoint, RequestMapping controllerMapping, PutMapping methodMapping,
                                     Throwable e) {
        logError(joinPoint, HttpMethod.PUT.name(), controllerMapping.value(), methodMapping.value(), e);
    }

    @AfterThrowing(value = "restControllerDeletePointcut(controllerMapping, methodMapping)", throwing = "e")
    public void logAfterRestThrowing(JoinPoint joinPoint, RequestMapping controllerMapping, DeleteMapping methodMapping,
                                     Throwable e) {
        logError(joinPoint, HttpMethod.DELETE.name(), controllerMapping.value(), methodMapping.value(), e);
    }

    @SuppressWarnings("squid:S1186") //suppress enpty method warning
    @Pointcut("@target(controllerMapping) && @annotation(methodMapping)")
    public void swaggerGeneratedEndpoint(RequestMapping controllerMapping, RequestMapping methodMapping) {
    }

    @Before("swaggerGeneratedEndpoint(controllerMapping, methodMapping)")
    public void logBeforeRest(JoinPoint joinPoint, RequestMapping controllerMapping, RequestMapping methodMapping) {
        logStart(joinPoint, renderMethod(methodMapping.method()), controllerMapping.value(), methodMapping.value());
    }

    @AfterReturning(value = "swaggerGeneratedEndpoint(controllerMapping, methodMapping)", returning = "result")
    public void logAfterRest(JoinPoint joinPoint, RequestMapping controllerMapping, RequestMapping methodMapping,
                             Object result) {
        logStop(joinPoint, renderMethod(methodMapping.method()), controllerMapping.value(), methodMapping.value(), result);
    }

    @AfterThrowing(value = "swaggerGeneratedEndpoint(controllerMapping, methodMapping)", throwing = "e")
    public void logAfterRestThrowing(JoinPoint joinPoint, RequestMapping controllerMapping, RequestMapping methodMapping,
                                     Throwable e) {
        logError(joinPoint, renderMethod(methodMapping.method()), controllerMapping.value(), methodMapping.value(), e);
    }

    private void logStart(final JoinPoint joinPoint, String method, final String[] controllerPath,
                          final String[] methodPath) {

        Signature signature = joinPoint.getSignature();
        String className = signature.getDeclaringTypeName();
        if (!withLogging(className)) {
            return;
        }

        Class declaringType = signature.getDeclaringType();
        className = declaringType.getSimpleName();
        String packageName = declaringType.getPackageName();
        String methodName = signature.getName();

        LogConfiguration config = loggingConfigService.getApiLoggingConfig(packageName, className, methodName);

        if (config == null) {
            log.info(LOG_START_PATTERN,
                     method,
                     LogObjectPrinter.joinUrlPaths(controllerPath, methodPath),
                     LogObjectPrinter.getCallMethod(joinPoint),
                     LogObjectPrinter.printInputParams(joinPoint));
            return;
        }

        if (OFF_LOG.equals(config.getLevel())) {
            return;
        }

        logWithLevel(log,
                     config.getLevel(),
                     LOG_START_PATTERN,
                     method,
                     LogObjectPrinter.joinUrlPaths(controllerPath, methodPath),
                     LogObjectPrinter.getCallMethod(joinPoint),
                     LogObjectPrinter.printInputParams(joinPoint, config.getLogInput()));
    }

    private void logStop(final JoinPoint joinPoint, String method, final String[] controllerPath,
                         final String[] methodPath,
                         final Object result) {

        Signature signature = joinPoint.getSignature();
        String className = signature.getDeclaringTypeName();
        if (!withLogging(className)) {
            return;
        }

        Class declaringType = signature.getDeclaringType();
        className = declaringType.getSimpleName();
        String packageName = declaringType.getPackageName();
        String methodName = signature.getName();

        LogConfiguration config = loggingConfigService.getApiLoggingConfig(packageName, className, methodName);

        if (config == null) {
            log.info(LOG_STOP_PATTERN,
                     method,
                     LogObjectPrinter.joinUrlPaths(controllerPath, methodPath),
                     LogObjectPrinter.getCallMethod(joinPoint),
                     WebLogObjectPrinter.printRestResult(joinPoint, result),
                     MdcUtils.getExecTimeMs());
            return;
        }

        if (OFF_LOG.equals(config.getLevel())) {
            return;
        }

        boolean printResult = DEFAULT_LOG_RESULT_DETAILS;
        if (Objects.nonNull(config.getLogResult()) && Objects.nonNull(config.getLogResult().getResultDetails())) {
            printResult = config.getLogResult().getResultDetails();
        }

        logWithLevel(log,
                         config.getLevel(),
                         LOG_STOP_PATTERN,
                         method,
                         LogObjectPrinter.joinUrlPaths(controllerPath, methodPath),
                         LogObjectPrinter.getCallMethod(joinPoint),
                         WebLogObjectPrinter.printRestResult(joinPoint, result, printResult),
                         MdcUtils.getExecTimeMs());
    }

    private void logError(final JoinPoint joinPoint, String method, final String[] controllerPath,
                          final String[] methodPath,
                          final Throwable e) {

        String className = joinPoint.getSignature().getDeclaringTypeName();
        if (!withLogging(className)) {
            return;
        }

        log.error(LOG_ERROR_PATTERN,
                  method,
                  LogObjectPrinter.joinUrlPaths(controllerPath, methodPath),
                  LogObjectPrinter.getCallMethod(joinPoint),
                  LogObjectPrinter.printExceptionWithStackInfo(e),
                  MdcUtils.getExecTimeMs());
    }

    private String renderMethod(RequestMethod[] method) {
        if (method != null && method.length == 1) {
            return method[0].name();
        }
        return Arrays.toString(method);
    }

}
