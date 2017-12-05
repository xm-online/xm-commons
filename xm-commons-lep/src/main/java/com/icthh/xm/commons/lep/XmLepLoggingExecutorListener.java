package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.LepExecutorEvent;
import com.icthh.xm.lep.api.LepExecutorEvent.AfterResourceExecutionEvent;
import com.icthh.xm.lep.api.LepExecutorEvent.BeforeResourceExecutionEvent;
import com.icthh.xm.lep.api.LepExecutorEvent.ResultObject;
import com.icthh.xm.lep.api.LepExecutorListener;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.MethodSignature;
import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Lep executor listener implementation (see {@link LepExecutorListener}) desired to print start, stop and error records
 * to log within LEP execution path.
 */
@Slf4j
public class XmLepLoggingExecutorListener implements LepExecutorListener {

    private static final String LOG_QUESTION = "?";

    private static final String LOG_SEMICOLON = ":";

    @Override
    public void accept(final LepExecutorEvent lepExecutorEvent) {
        if (lepExecutorEvent instanceof BeforeResourceExecutionEvent) {
            onBeforeEvent((BeforeResourceExecutionEvent) lepExecutorEvent);
        } else if (lepExecutorEvent instanceof AfterResourceExecutionEvent) {
            onAfterEvent((AfterResourceExecutionEvent) lepExecutorEvent);
        }

    }

    private void onBeforeEvent(BeforeResourceExecutionEvent beforeEvent) {

        log.info("lep:start: execute lep at [{}], script: {}",
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
            logStop(signatureToPrint, scriptName);
        }

    }

    private void logStop(String signature, String scriptName) {
        log.info("lep:stop:  execute lep at [{}], script: {}",
                 signature,
                 scriptName);
    }

    private void logStopError(String signature, String scriptName, Exception e) {
        log.error("lep:stop:  execute lep error at [{}], script: {}, error: {}",
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
