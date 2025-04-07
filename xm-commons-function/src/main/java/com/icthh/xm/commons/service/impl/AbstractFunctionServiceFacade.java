package com.icthh.xm.commons.service.impl;

import static com.icthh.xm.commons.utils.Constants.FUNCTION_CALL_PRIVILEGE;
import static com.icthh.xm.commons.utils.HttpRequestUtils.convertToCanonicalHttpMethod;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.icthh.xm.commons.domain.FunctionResult;
import com.icthh.xm.commons.domain.enums.FunctionTxTypes;
import com.icthh.xm.commons.domain.spec.IFunctionSpec;
import com.icthh.xm.commons.service.FunctionExecutorService;
import com.icthh.xm.commons.service.FunctionResultProcessor;
import com.icthh.xm.commons.service.FunctionService;
import com.icthh.xm.commons.service.FunctionServiceFacade;
import com.icthh.xm.commons.service.FunctionTxControl;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.security.access.AccessDeniedException;

@RequiredArgsConstructor
public abstract class AbstractFunctionServiceFacade<FS extends IFunctionSpec> implements FunctionServiceFacade {

    private final FunctionService<FS> functionService;
    private final FunctionTxControl functionTxControl;
    private final FunctionExecutorService functionExecutorService;
    private final FunctionResultProcessor<FS> functionResultProcessor;

    @Override
    public FunctionResult execute(String functionKey, Map<String, Object> functionInput, String httpMethod) {
        StopWatch stopWatch = StopWatch.createStarted();
        functionService.validateFunctionKey(functionKey);

        FS functionSpec = functionService.findFunctionSpec(functionKey, httpMethod);
        String functionSpecFromSpec = functionSpec.getKey();
        functionService.checkPermissions(FUNCTION_CALL_PRIVILEGE, functionSpecFromSpec);

        Map<String, Object> input = functionService.getValidFunctionInput(functionSpec, functionInput);
        functionService.enrichInputFromPathParams(functionKey, input, functionSpec);

        FunctionResult functionResult = callLepExecutor(functionSpec.getTxType(), () -> {
            var lepHttpMethod = convertToCanonicalHttpMethod(httpMethod);
            Object data = functionExecutorService.execute(functionSpecFromSpec, input, lepHttpMethod);
            return processFunctionResult(functionSpecFromSpec, data, functionSpec);
        });
        functionResult.setExecuteTime(stopWatch.getTime(MILLISECONDS));
        return functionResult;
    }

    @Override
    public FunctionResult executeAnonymous(String functionKey, Map<String, Object> functionInput, String httpMethod) {
        StopWatch stopWatch = StopWatch.createStarted();
        FS functionSpec = functionService.findFunctionSpec(functionKey, httpMethod);

        if (!functionSpec.getAnonymous()) {
            throw new AccessDeniedException("Access denied");
        }
        functionService.validateFunctionKey(functionKey);
        Map<String, Object> input = functionService.getValidFunctionInput(functionSpec, functionInput);
        functionService.enrichInputFromPathParams(functionKey, input, functionSpec);

        FunctionResult functionResult = callLepExecutor(functionSpec.getTxType(), () -> {
            var lepHttpMethod = convertToCanonicalHttpMethod(httpMethod);
            Object data = functionExecutorService.executeAnonymousFunction(functionSpec.getKey(), input, lepHttpMethod);
            return processFunctionResult(functionKey, data, functionSpec);
        });
        functionResult.setExecuteTime(stopWatch.getTime(MILLISECONDS));
        return functionResult;
    }

    /**
     * Invoke functionExecutorService in one of available Transaction Contexts
     *
     * @param txType FunctionTxTypes
     * @param logic  functionExecutorService implementation for lep execution
     * @return function call result
     */
    public FunctionResult callLepExecutor(FunctionTxTypes txType, Supplier<FunctionResult> logic) {
        return switch (txType) {
            case READ_ONLY -> functionTxControl.executeInTransactionWithRoMode(logic);
            case NO_TX -> functionTxControl.executeWithNoTx(logic);
            default -> functionTxControl.executeInTransaction(logic);
        };
    }

    private FunctionResult processFunctionResult(String functionKey, Object data, FS functionSpec) {
        return functionResultProcessor.processFunctionResult(functionKey, data, functionSpec);
    }
}
