package com.icthh.xm.commons.lep;

import com.icthh.xm.commons.logging.config.LoggingConfig.LepLogConfiguration;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import com.icthh.xm.lep.api.LepExecutorEvent;
import com.icthh.xm.lep.api.LepExecutorEvent.AfterResourceExecutionEvent;
import com.icthh.xm.lep.api.LepExecutorEvent.BeforeResourceExecutionEvent;
import com.icthh.xm.lep.api.LepExecutorEvent.ResultObject;
import com.icthh.xm.lep.api.LepExecutorListener;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.MethodSignature;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;

import static com.icthh.xm.commons.logging.util.LogObjectPrinter.Level.OFF_LOG;
import static com.icthh.xm.commons.logging.util.LogObjectPrinter.setLevelAndPrint;

/**
 * Lep executor listener implementation (see {@link LepExecutorListener}) desired to print start, stop and error records
 * to log within LEP execution path.
 */
@Slf4j
public class XmLepLoggingExecutorListener implements LepExecutorListener {

    private static final String LOG_QUESTION = "?";
    private static final String LOG_SEMICOLON = ":";
    private static final String LOG_START_PATTERN = "lep:start: execute lep at [{}], script: {}";
    private static final String LOG_STOP_PATTERN = "lep:stop:  execute lep at [{}], script: {}";
    private static final String LOG_ERROR_PATTERN = "lep:stop:  execute lep error at [{}], script: {}, error: {}";

    private final LoggingConfigService loggingConfigService;

    public XmLepLoggingExecutorListener(LoggingConfigService loggingConfigService) {
        this.loggingConfigService = loggingConfigService;
    }

    @Override
    public void accept(final LepExecutorEvent lepExecutorEvent) {
        if (lepExecutorEvent instanceof BeforeResourceExecutionEvent) {
            onBeforeEvent((BeforeResourceExecutionEvent) lepExecutorEvent);
        } else if (lepExecutorEvent instanceof AfterResourceExecutionEvent) {
            onAfterEvent((AfterResourceExecutionEvent) lepExecutorEvent);
        }

    }

    private void onBeforeEvent(BeforeResourceExecutionEvent beforeEvent) {

        LepLogConfiguration loggingConfig = loggingConfigService.getLepLoggingConfig(beforeEvent.getKey().getId());

        if (Objects.isNull(loggingConfig)) {
            log.info(LOG_START_PATTERN,
                     buildLepSignature(beforeEvent.getMethod()),
                     beforeEvent.getKey());
            return;
        }

        if (OFF_LOG.equals(loggingConfig.getLevel())) {
            return;
        }

        setLevelAndPrint(log, loggingConfig.getLevel(),
                         LOG_START_PATTERN,
                         buildLepSignature(beforeEvent.getMethod()),
                         beforeEvent.getKey());
    }

    private void onAfterEvent(AfterResourceExecutionEvent afterEvent) {
        String signatureToPrint = buildLepSignature(afterEvent.getMethod());
        String scriptName = afterEvent.getKey().getId();

        Optional<Exception> exception = afterEvent.getResult().flatMap(ResultObject::getException);

        if (exception.isPresent()) {
            logStopError(signatureToPrint, scriptName, exception.get());
        } else {
            LepLogConfiguration loggingConfig = loggingConfigService.getLepLoggingConfig(afterEvent.getKey().getId());
            logStop(signatureToPrint, scriptName, loggingConfig);
        }

    }

    private void logStop(String signature, String scriptName, LepLogConfiguration config) {
        if (Objects.isNull(config)) {
            log.info(LOG_STOP_PATTERN, signature, scriptName);
            return;
        }
        if (OFF_LOG.equals(config.getLevel())) {
            return;
        }
        setLevelAndPrint(log, config.getLevel(), LOG_STOP_PATTERN, signature, scriptName);
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
