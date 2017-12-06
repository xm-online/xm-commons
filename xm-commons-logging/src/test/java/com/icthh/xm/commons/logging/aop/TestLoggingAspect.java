package com.icthh.xm.commons.logging.aop;

import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Logging aspect for test. Is used to debug and test sprint PointCuts.
 */
@Aspect
public class TestLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(TestLoggingAspect.class);

    public static final String PROCESSED_KEY = "PROCESSED_KEY";

    public static final String PROCESSED_CONFIG = "PROCESSED_CONFIG";

    Map<String, Object> processedData = new HashMap<>();

    @SuppressWarnings("squid:S1186") //suppress empty method warning
    @Pointcut("execution(* com.icthh.xm.commons.logging.aop.TestServiceForLogging+.*(String, String))"
              + " && args(updatedKey, config)")
    public void onRefreshInitPointcut(String updatedKey, String config) {

    }

    @Around("onRefreshInitPointcut(updatedKey, config)")
    public Object logBeforeService(ProceedingJoinPoint joinPoint, String updatedKey, String config) throws Throwable {

        log.info("process joinpoint [{}], updateKey = {}, confilg = {}", LogObjectPrinter.getCallMethod(joinPoint),
                 updatedKey, config);

        processedData.put(PROCESSED_KEY, updatedKey);
        processedData.put(PROCESSED_CONFIG, config);

        return joinPoint.proceed();

    }

    public String getProcessedKey() {
        return (String) processedData.get(PROCESSED_KEY);
    }

    public String getProcessedConfig() {
        return (String) processedData.get(PROCESSED_CONFIG);
    }

}
