package com.icthh.xm.commons.service;

import com.icthh.xm.commons.domain.spec.FunctionSpec;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.icthh.xm.commons.utils.Constants.FUNCTION_CALL_PRIVILEGE;

@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionExportServiceFacadeImpl implements FunctionExportServiceFacade {

    private final FunctionService<FunctionSpec> functionService;
    private final FunctionTxControl functionTxControl;
    private final FunctionExecutorWrapper functionExecutorWrapper;

    @Override
    public void execute(String functionKey, String fileFormat, Map<String, Object> functionInput, HttpServletResponse response) {
        functionService.validateFunctionKey(functionKey);

        FunctionSpec functionSpec = functionService.findFunctionSpec(functionKey, HttpMethod.GET.name());
        String functionSpecKey = functionSpec.getKey();
        functionService.checkPermissions(FUNCTION_CALL_PRIVILEGE, functionSpecKey);

        Map<String, Object> input = functionService.getValidFunctionInput(functionSpec, functionInput);
        functionService.enrichInputFromPathParams(functionKey, input, functionSpec);

        // execute in READ_ONLY mode
        functionTxControl.executeInTransactionWithRoMode(() ->
            functionExecutorWrapper.execute(functionSpecKey, fileFormat, input, response)
        );
    }
}
