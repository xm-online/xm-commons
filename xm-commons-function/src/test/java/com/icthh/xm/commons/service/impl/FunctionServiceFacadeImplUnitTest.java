package com.icthh.xm.commons.service.impl;

import com.icthh.xm.commons.domain.FunctionResult;
import com.icthh.xm.commons.domain.spec.FunctionSpec;
import com.icthh.xm.commons.service.FunctionExecutorService;
import com.icthh.xm.commons.service.FunctionResultProcessor;
import com.icthh.xm.commons.service.FunctionService;
import com.icthh.xm.commons.service.FunctionTxControl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;
import java.util.function.Supplier;

import static com.icthh.xm.commons.domain.enums.FunctionTxTypes.NO_TX;
import static com.icthh.xm.commons.domain.enums.FunctionTxTypes.READ_ONLY;
import static com.icthh.xm.commons.domain.enums.FunctionTxTypes.TX;
import static com.icthh.xm.commons.utils.Constants.FUNCTION_CALL_PRIVILEGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

public class FunctionServiceFacadeImplUnitTest {

    private static final String FUNCTION_KEY_TEST = "FUNCTION.PACKAGE-TEST";

    @Mock
    private FunctionService<FunctionSpec> functionService;

    @Mock
    private FunctionTxControl functionTxControl;

    @Mock
    private FunctionExecutorService functionExecutorService;

    @Mock
    private FunctionResultProcessor<FunctionSpec> functionResultProcessor;

    private AbstractFunctionServiceFacade<FunctionSpec> functionServiceFacade;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        functionServiceFacade = new AbstractFunctionServiceFacade<>(
            functionService, functionTxControl, functionExecutorService, functionResultProcessor
        ) {};
    }

    @Test
    void execute() {
        FunctionSpec mockFunctionSpec = mock(FunctionSpec.class);
        when(mockFunctionSpec.getTxType()).thenReturn(TX); // to execute in default transaction
        when(mockFunctionSpec.getKey()).thenReturn(FUNCTION_KEY_TEST);

        Map<String, Object> functionInput = Map.of("param", "value");
        Map<String, Object> validatedInput = Map.of("param", "value");

        Map<String, Object> executionData = Map.of("result", "success");
        FunctionResult expectedResult = mock(FunctionResult.class);

        when(functionService.findFunctionSpec(FUNCTION_KEY_TEST, GET.name())).thenReturn(mockFunctionSpec);
        when(functionService.getValidFunctionInput(mockFunctionSpec, functionInput)).thenReturn(validatedInput);
        when(functionExecutorService.execute(FUNCTION_KEY_TEST, validatedInput, GET.name())).thenReturn(executionData);
        when(functionResultProcessor.processFunctionResult(FUNCTION_KEY_TEST, executionData, mockFunctionSpec))
            .thenReturn(expectedResult);
        when(functionTxControl.executeInTransaction(any(Supplier.class)))
            .thenAnswer(invocation -> ((Supplier<FunctionResult>) invocation.getArgument(0)).get());

        FunctionResult result = functionServiceFacade.execute(FUNCTION_KEY_TEST, functionInput, GET.name());
        assertEquals(expectedResult, result);

        verify(functionService).validateFunctionKey(FUNCTION_KEY_TEST);
        verify(functionService).checkPermissions(FUNCTION_CALL_PRIVILEGE, FUNCTION_KEY_TEST);
        verify(functionService).enrichInputFromPathParams(FUNCTION_KEY_TEST, validatedInput, mockFunctionSpec);
    }

    @Test
    void executeWithPath() {
        String functionInputPath = "testEntity/111/activate";
        String functionSpecPath = "testEntity/{id}/activate";

        FunctionSpec mockFunctionSpec = mock(FunctionSpec.class);
        when(mockFunctionSpec.getTxType()).thenReturn(TX); // to execute in default transaction
        when(mockFunctionSpec.getKey()).thenReturn(FUNCTION_KEY_TEST);
        when(mockFunctionSpec.getPath()).thenReturn(functionSpecPath);

        Map<String, Object> functionInput = Map.of("param", "value");
        Map<String, Object> validatedInput = Map.of("param", "value");

        Map<String, Object> executionData = Map.of("result", "success");
        FunctionResult expectedResult = mock(FunctionResult.class);

        when(functionService.findFunctionSpec(functionInputPath, GET.name())).thenReturn(mockFunctionSpec);
        when(functionService.getValidFunctionInput(mockFunctionSpec, functionInput)).thenReturn(validatedInput);
        when(functionExecutorService.execute(FUNCTION_KEY_TEST, validatedInput, GET.name())).thenReturn(executionData);
        when(functionResultProcessor.processFunctionResult(FUNCTION_KEY_TEST, executionData, mockFunctionSpec))
            .thenReturn(expectedResult);
        when(functionTxControl.executeInTransaction(any(Supplier.class)))
            .thenAnswer(invocation -> ((Supplier<FunctionResult>) invocation.getArgument(0)).get());

        FunctionResult result = functionServiceFacade.execute(functionInputPath, functionInput, GET.name());
        assertEquals(expectedResult, result);

        verify(functionService).validateFunctionKey(functionInputPath);
        verify(functionService).checkPermissions(FUNCTION_CALL_PRIVILEGE, FUNCTION_KEY_TEST);
        verify(functionService).enrichInputFromPathParams(functionInputPath, validatedInput, mockFunctionSpec);
    }

    @Test
    void executeAnonymous_accessDenied() {
        FunctionSpec mockFunctionSpec = mock(FunctionSpec.class);
        when(mockFunctionSpec.getAnonymous()).thenReturn(false);

        when(functionService.findFunctionSpec(FUNCTION_KEY_TEST, POST.name())).thenReturn(mockFunctionSpec);

        assertThrows(AccessDeniedException.class,
            () -> functionServiceFacade.executeAnonymous(FUNCTION_KEY_TEST, Map.of(), POST.name()),
            "Access denied");
    }

    @Test
    void testCallLepExecutorWithReadOnlyTx() {
        FunctionResult expectedResult = mock(FunctionResult.class);
        Supplier<FunctionResult> logic = mock(Supplier.class);

        when(functionTxControl.executeInTransactionWithRoMode(logic)).thenReturn(expectedResult);

        FunctionResult result = functionServiceFacade.callLepExecutor(READ_ONLY, logic);

        verify(functionTxControl).executeInTransactionWithRoMode(logic);
        assertEquals(expectedResult, result);
    }

    @Test
    void testCallLepExecutorWithNoTx() {
        FunctionResult expectedResult = mock(FunctionResult.class);
        Supplier<FunctionResult> logic = mock(Supplier.class);

        when(functionTxControl.executeWithNoTx(logic)).thenReturn(expectedResult);

        FunctionResult result = functionServiceFacade.callLepExecutor(NO_TX, logic);

        verify(functionTxControl).executeWithNoTx(logic);
        assertEquals(expectedResult, result);
    }
}
