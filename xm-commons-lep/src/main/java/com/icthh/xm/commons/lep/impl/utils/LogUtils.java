package com.icthh.xm.commons.lep.impl.utils;

import com.icthh.xm.commons.logging.config.LoggingConfig;
import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.MethodSignature;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.icthh.xm.commons.logging.util.LogObjectPrinter.Level.OFF_LOG;
import static com.icthh.xm.commons.logging.util.LogObjectPrinter.logWithLevel;

@Slf4j
public class LogUtils {

    private static final String LOG_QUESTION = "?";
    private static final String LOG_SEMICOLON = ":";
    private static final String LOG_START_PATTERN = "lep:start: execute lep at [{}], script: {}";
    private static final String LOG_STOP_PATTERN = "lep:stop:  execute lep at [{}], script: {}";
    private static final String LOG_ERROR_PATTERN = "lep:stop:  execute lep error at [{}], script: {}, error: {}";

    private void logStop(String signature, String scriptName, LoggingConfig.LepLogConfiguration config) {
        if (config == null) {
            log.info(LOG_STOP_PATTERN, signature, scriptName);
            return;
        }
        if (OFF_LOG.equals(config.getLevel())) {
            return;
        }
        logWithLevel(log, config.getLevel(), LOG_STOP_PATTERN, signature, scriptName);
    }

    private void logStopError(String signature, String scriptName, Exception e) {
        log.error(LOG_ERROR_PATTERN,
            signature,
            scriptName,
            LogObjectPrinter.printExceptionWithStackInfo(e));
    }

    /**
     * Null safely print class and method name fro Lep {@link MethodSignature}
     *
     * @param method LEP method
     * @return class and method string representation.
     */
    private static String buildLepSignature(LepMethod method) {

        String className = Optional.ofNullable(method)
            .map(LepMethod::getMethodSignature)
            .map(MethodSignature::getDeclaringClass)
            .map(Class::getSimpleName).orElse(LOG_QUESTION);

        String methodName = Optional.ofNullable(method)
            .map(LepMethod::getMethodSignature)
            .map(MethodSignature::getName).orElse(LOG_QUESTION);

        return className + LOG_SEMICOLON + methodName;
    }

}
