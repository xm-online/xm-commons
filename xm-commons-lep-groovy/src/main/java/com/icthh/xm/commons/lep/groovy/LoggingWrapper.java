package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.api.LepKey;
import com.icthh.xm.commons.logging.config.LoggingConfig.LepLogConfiguration;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.MethodSignature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.Callable;

import static com.icthh.xm.commons.logging.util.LogObjectPrinter.Level.OFF_LOG;
import static com.icthh.xm.commons.logging.util.LogObjectPrinter.logWithLevel;

@Slf4j
@RequiredArgsConstructor
public class LoggingWrapper {

    private static final String LOG_QUESTION = "?";
    private static final String LOG_SEMICOLON = ":";
    private static final String LOG_START_PATTERN = "lep:start: execute lep at [{}], script: {}";
    private static final String LOG_STOP_PATTERN = "lep:stop:  execute lep at [{}], script: {}";
    private static final String LOG_ERROR_PATTERN = "lep:stop:  execute lep error at [{}], script: {}, error: {}";

    private final LoggingConfigService loggingConfigService;

    public <T> T doWithLogs(LepMethod lepMethod, String scriptName, LepKey lepKey, Callable<T> task) throws Exception {
        String methodSignature = buildLepSignature(lepMethod);
        try {
            logStart(methodSignature, scriptName);
            T result = task.call();
            logStop(methodSignature, scriptName);
            return result;
        } catch (Throwable e) {
            logStopError(methodSignature, scriptName, e);
            throw e;
        }
    }

    private void logStart(String methodSignature, String scriptName) {

        LepLogConfiguration loggingConfig = loggingConfigService.getLepLoggingConfig(scriptName);

        if (loggingConfig == null) {
            log.info(LOG_START_PATTERN,
                methodSignature,
                scriptName);
            return;
        }

        if (OFF_LOG.equals(loggingConfig.getLevel())) {
            return;
        }

        logWithLevel(log, loggingConfig.getLevel(),
            LOG_START_PATTERN,
            methodSignature,
            scriptName);
    }

    private void logStop(String signature, String scriptName) {
        LepLogConfiguration config = loggingConfigService.getLepLoggingConfig(scriptName);
        if (config == null) {
            log.info(LOG_STOP_PATTERN, signature, scriptName);
            return;
        }
        if (OFF_LOG.equals(config.getLevel())) {
            return;
        }
        logWithLevel(log, config.getLevel(), LOG_STOP_PATTERN, signature, scriptName);
    }

    private void logStopError(String signature, String scriptName, Throwable e) {
        log.error(LOG_ERROR_PATTERN,
            signature,
            scriptName,
            LogObjectPrinter.printExceptionWithStackInfo(e));
    }

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
