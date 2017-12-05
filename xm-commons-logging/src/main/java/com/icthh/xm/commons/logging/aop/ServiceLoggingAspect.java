package com.icthh.xm.commons.logging.aop;

import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.concurrent.TimeUnit;

/**
 * Aspect for Service logging.
 */
@Aspect
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class ServiceLoggingAspect {

    @SuppressWarnings("squid:S1186") //suppress empty method warning
    @Pointcut("@annotation(com.icthh.xm.commons.logging.aop.IgnoreLogginAspect) "
        + "|| within(@com.icthh.xm.commons.logging.aop.IgnoreLogginAspect *)")
    public void excluded() {
    }

    @SuppressWarnings("squid:S1186") //suppress empty method warning
    @Pointcut("within(com.icthh.xm..*) && (within(@org.springframework.stereotype.Service *) "
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

        StopWatch stopWatch = StopWatch.createStarted();

        try {
            logStart(joinPoint);

            Object result = joinPoint.proceed();

            logStop(joinPoint, result, stopWatch);

            return result;
        } catch (Exception e) {
            logError(joinPoint, e, stopWatch);
            throw e;
        }

    }

    private void logStart(final JoinPoint joinPoint) {
        log.info("srv:start: {}, input: {}",
                 LogObjectPrinter.getCallMethod(joinPoint),
                 LogObjectPrinter.printInputParams(joinPoint));
    }

    private void logStop(final JoinPoint joinPoint, final Object result, final StopWatch stopWatch) {
        log.info("srv:stop:  {}, result: {}, time = {} ms",
                 LogObjectPrinter.getCallMethod(joinPoint),
                 LogObjectPrinter.printResult(joinPoint, result),
                 stopWatch.getTime(TimeUnit.MILLISECONDS));
    }

    private void logError(final JoinPoint joinPoint, final Throwable e, final StopWatch stopWatch) {
        log.error("srv:stop:  {}, error: {}, time = {} ms",
                  LogObjectPrinter.getCallMethod(joinPoint),
                  LogObjectPrinter.printExceptionWithStackInfo(e),
                  stopWatch.getTime(TimeUnit.MILLISECONDS));
    }
}
