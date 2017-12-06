package com.icthh.xm.commons.logging.web.aop;

import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import com.icthh.xm.commons.logging.web.util.WebLogObjectPrinter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.concurrent.TimeUnit;

/**
 *
 */
@Aspect
@Slf4j
public class RestCallLoggingAspect {

    @Pointcut("within(org.springframework.web.client.RestTemplate)")
    public void restTemplatePointcut() {
    }

    @Around("restTemplatePointcut()")
    public Object logBeforeRest(ProceedingJoinPoint joinPoint) throws Throwable {

        StopWatch stopWatch = StopWatch.createStarted();

        String uri = null;
        String calledApi = null;
        try {

            uri = LogObjectPrinter.printInputParams(joinPoint, "method", "url");
            calledApi = LogObjectPrinter.getCallMethod(joinPoint);

            log.info("rest:start {} --> {}", calledApi, uri);

            Object result = joinPoint.proceed();

            log.info("rest:stop  {} --> {}, {}, time = {} ms",
                     calledApi,
                     uri,
                     WebLogObjectPrinter.printRestResult(joinPoint, result, false),
                     stopWatch.getTime(TimeUnit.MILLISECONDS));

            return result;

        } catch (Exception e) {
            log.error("rest:stop  {} --> request = {}, error = {}, time = {} ms",
                      calledApi,
                      LogObjectPrinter.printInputParams(joinPoint),
                      LogObjectPrinter.printExceptionWithStackInfo(e),
                      stopWatch.getTime(TimeUnit.MILLISECONDS));
            throw e;
        }
    }

}
