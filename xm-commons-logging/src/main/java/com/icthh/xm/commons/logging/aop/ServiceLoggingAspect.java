package com.icthh.xm.commons.logging.aop;

import com.icthh.xm.commons.logging.config.LoggingConfig.LogConfiguration;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.icthh.xm.commons.logging.util.LogObjectPrinter.Level.OFF_LOG;
import static com.icthh.xm.commons.logging.util.LogObjectPrinter.getCallMethod;
import static com.icthh.xm.commons.logging.util.LogObjectPrinter.printExceptionWithStackInfo;
import static com.icthh.xm.commons.logging.util.LogObjectPrinter.printInputParams;
import static com.icthh.xm.commons.logging.util.LogObjectPrinter.printResult;
import static com.icthh.xm.commons.logging.util.LogObjectPrinter.setLevelAndPrint;

/**
 * Aspect for Service logging.
 */
@Component
@Aspect
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class ServiceLoggingAspect {

    private static final String XM_BASE_PACKAGE = "com.icthh.xm";
    private static final String LOG_START_PATTERN = "srv:start: {}, input: {}";
    private static final String LOG_STOP_PATTERN = "srv:stop:  {}, result: {}, time = {} ms";
    private static final String LOG_ERROR_PATTERN = "srv:stop:  {}, error: {}, time = {} ms";

    private final LoggingConfigService loggingConfigService;

    @Value("${base-package:'-'}")
    private String basePackage;

    @SuppressWarnings("squid:S1186") //suppress empty method warning
    @Pointcut("@annotation(com.icthh.xm.commons.logging.aop.IgnoreLogginAspect) "
        + "|| within(@com.icthh.xm.commons.logging.aop.IgnoreLogginAspect *)")
    public void excluded() {
    }

    @SuppressWarnings("squid:S1186") //suppress empty method warning
    @Pointcut("(within(@org.springframework.stereotype.Service *) "
        + "|| within(@com.icthh.xm.commons.lep.*.LepService *))")
    public void servicePointcut() {
    }

    /**
     * Aspect for logging before service calls.
     *
     * @param joinPoint joinPoint
     * @return method result
     * @throws Throwable throwable
     */
    @SneakyThrows
    @Around("servicePointcut() && !excluded()")
    public Object logBeforeService(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringTypeName();

        if (!withLogging(className)) {
            return joinPoint.proceed();
        }

        className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String packageName = joinPoint.getSignature().getDeclaringType().getPackageName();
        String methodName = joinPoint.getSignature().getName();

        LogConfiguration config = loggingConfigService.getServiceLoggingConfig(packageName,
            className,
            methodName);

        StopWatch stopWatch = StopWatch.createStarted();

        try {
            logStart(joinPoint, config);
            Object result = joinPoint.proceed();
            logStop(joinPoint, result, stopWatch, config);
            return result;
        } catch (Exception e) {
            logError(joinPoint, e, stopWatch);
            throw e;
        }
    }


    private boolean withLogging(String className) {
        return className.startsWith(XM_BASE_PACKAGE) || className.startsWith(basePackage);
    }


    private void logStart(final JoinPoint joinPoint, LogConfiguration config) {
        String callMethod = getCallMethod(joinPoint);
        if (Objects.isNull(config)) {
            log.info(LOG_START_PATTERN,
                callMethod,
                printInputParams(joinPoint));
            return;
        }

        if (OFF_LOG.equals(config.getLevel())) {
            return;
        }

        setLevelAndPrint(log, config.getLevel(),
            LOG_START_PATTERN,
            callMethod,
            printInputParams(joinPoint, config.getLogInput()));

    }

    private void logStop(final JoinPoint joinPoint, final Object result, final StopWatch stopWatch, LogConfiguration config) {
        String callMethod = getCallMethod(joinPoint);
        if (Objects.isNull(config)) {
            log.info(LOG_STOP_PATTERN,
                callMethod,
                printResult(joinPoint, result),
                stopWatch.getTime(TimeUnit.MILLISECONDS));
            return;
        }

        if (OFF_LOG.equals(config.getLevel())) {
            return;
        }

        setLevelAndPrint(log, config.getLevel(),
            LOG_STOP_PATTERN,
            callMethod,
            printResult(joinPoint, result, config.getLogResult()),
            stopWatch.getTime(TimeUnit.MILLISECONDS));

    }

    private void logError(final JoinPoint joinPoint, final Throwable e, final StopWatch stopWatch) {
        log.error(LOG_ERROR_PATTERN,
            getCallMethod(joinPoint),
            printExceptionWithStackInfo(e),
            stopWatch.getTime(TimeUnit.MILLISECONDS));
    }

}
