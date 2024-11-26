package com.icthh.xm.commons.service.impl;

import com.icthh.xm.commons.domain.FunctionResult;
import com.icthh.xm.commons.domain.enums.FunctionTxTypes;
import com.icthh.xm.commons.service.FunctionExecutorService;
import com.icthh.xm.commons.service.FunctionResultProcessor;
import com.icthh.xm.commons.service.FunctionService;
import com.icthh.xm.commons.service.FunctionServiceFacade;
import com.icthh.xm.commons.service.FunctionTxControl;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Supplier;

import static com.icthh.xm.commons.utils.HttpRequestUtils.convertToCanonicalHttpMethod;

@Service
@RequiredArgsConstructor
@ConditionalOnMissingBean(FunctionServiceFacade.class)
public class FunctionServiceFacadeImpl<FS> implements FunctionServiceFacade {

    public static String FUNCTION_CALL_PRIV = "FUNCTION.CALL"; // todo: is it possible to get from spec ???

    private final FunctionService<FS> functionService;
    private final FunctionTxControl functionTxControl;
    private final FunctionExecutorService functionExecutorService;
    private final FunctionResultProcessor<FS> functionResultProcessor;

    @Override
    public FunctionResult execute(String functionKey, Map<String, Object> functionInput, String httpMethod) {
        functionService.validateFunctionKey(functionKey);
        functionService.checkPermissions(FUNCTION_CALL_PRIV, functionKey);

        FS functionSpec = functionService.findFunctionSpec(functionKey, httpMethod);
        Map<String, Object> input = functionService.getValidFunctionInput(functionSpec, functionInput);
        functionService.enrichInputFromPathParams(functionKey, input, functionSpec);

        FunctionTxTypes txType = functionService.getTxType(functionSpec);

        return callLepExecutor(txType, () -> {
            var lepHttpMethod = convertToCanonicalHttpMethod(httpMethod);
            Map<String, Object> data = functionExecutorService.execute(functionKey, input, lepHttpMethod);
            return processFunctionResult(functionKey, data, functionSpec);
        });
    }

    @Override
    public FunctionResult executeAnonymous(String functionKey, Map<String, Object> functionInput, String httpMethod) {
        FS functionSpec = functionService.findFunctionSpec(functionKey, httpMethod);

        if (!functionService.isAnonymous(functionSpec)) {
            throw new AccessDeniedException("Access denied");
        }
        functionService.validateFunctionKey(functionKey);
        Map<String, Object> input = functionService.getValidFunctionInput(functionSpec, functionInput);
        functionService.enrichInputFromPathParams(functionKey, input, functionSpec);

        FunctionTxTypes txType = functionService.getTxType(functionSpec);

        return callLepExecutor(txType, () -> {
            var lepHttpMethod = convertToCanonicalHttpMethod(httpMethod);
            Map<String, Object> data = functionExecutorService.executeAnonymousFunction(functionKey, input, lepHttpMethod);
            return processFunctionResult(functionKey, data, functionSpec);
        });
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

    private FunctionResult processFunctionResult(String functionKey, Map<String, Object> data, FS functionSpec) {
        return functionResultProcessor.processFunctionResult(functionKey, data, functionSpec);
    }
}
