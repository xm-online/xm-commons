package com.icthh.xm.commons.service.impl;

import com.icthh.xm.commons.config.FunctionApiSpecConfiguration;
import com.icthh.xm.commons.domain.enums.FunctionFeatureContext;
import com.icthh.xm.commons.domain.spec.FunctionApiSpec;
import com.icthh.xm.commons.permission.domain.enums.IFeatureContext;
import com.icthh.xm.commons.permission.service.DynamicPermissionCheckService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.utils.JsonValidationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.commons.utils.Constants.FUNCTION_CALL_PRIVILEGE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;

public class FunctionServiceImplUnitTest {

    private static final String TEST_TENANT = "TEST_TENANT";
    private static final String FUNCTION_KEY_TEST = "FUNCTION.PACKAGE-TEST";

    @Mock
    private DynamicPermissionCheckService dynamicPermissionCheckService;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private FunctionApiSpecConfiguration functionApiSpecConfiguration;

    @InjectMocks
    private FunctionServiceImpl functionService;

    private IFeatureContext featureContext = mock(FunctionFeatureContext.class);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void validateFunctionKey() {
        assertDoesNotThrow(() -> functionService.validateFunctionKey("validKey"));
    }

    @Test
    void validateFunctionKey_invalidKey() {
        assertThrows(NullPointerException.class,
            () -> functionService.validateFunctionKey(null),
            "functionKey can't be null");
    }

    @Test
    void checkPermissions() {
        functionService.checkPermissions(featureContext, FUNCTION_CALL_PRIVILEGE, FUNCTION_KEY_TEST);

        verify(dynamicPermissionCheckService).checkContextPermission(featureContext, FUNCTION_CALL_PRIVILEGE, FUNCTION_KEY_TEST);
    }

    @Test
    void checkPermissions_defaultFeatureContext() {
        functionService.checkPermissions(FUNCTION_CALL_PRIVILEGE, FUNCTION_KEY_TEST);

        verify(dynamicPermissionCheckService).checkContextPermission(FunctionFeatureContext.FUNCTION, FUNCTION_CALL_PRIVILEGE, FUNCTION_KEY_TEST);
    }

    @Test
    void enrichInputFromPathParams() {
        FunctionApiSpec functionSpec = mock(FunctionApiSpec.class);
        when(functionSpec.getPath()).thenReturn("call/function/by-path/{id}");

        String functionKey = "call/function/by-path/FUNCTION";
        Map<String, Object> functionInput = new HashMap<>();

        functionService.enrichInputFromPathParams(functionKey, functionInput, functionSpec);

        assertEquals(1, functionInput.size());
        assertTrue(functionInput.containsKey("id"));
        assertEquals("FUNCTION", functionInput.get("id"));
    }

    @Test
    void enrichInputFromPathParams_withNonMatchingPath() {
        FunctionApiSpec functionSpec = mock(FunctionApiSpec.class);
        when(functionSpec.getPath()).thenReturn("call/function/by-path/{id}");

        String functionKey = "/different/path";
        Map<String, Object> functionInput = new HashMap<>();

        functionService.enrichInputFromPathParams(functionKey, functionInput, functionSpec);

        assertTrue(functionInput.isEmpty());
    }

    @Test
    void enrichInputFromPathParams_withNullPath() {
        FunctionApiSpec functionSpec = mock(FunctionApiSpec.class);
        when(functionSpec.getPath()).thenReturn(null);

        String functionKey = "call/function/by-path/FUNCTION";
        Map<String, Object> functionInput = new HashMap<>();

        functionService.enrichInputFromPathParams(functionKey, functionInput, functionSpec);

        assertTrue(functionInput.isEmpty());
    }

    @Test
    void findFunctionSpec() {
        FunctionApiSpec functionSpec = mock(FunctionApiSpec.class);

        when(tenantContextHolder.getTenantKey()).thenReturn(TEST_TENANT);
        when(functionApiSpecConfiguration.getSpecByTenant(TEST_TENANT)).thenReturn(Optional.of(functionSpec));

        FunctionApiSpec result = functionService.findFunctionSpec("functionKey", GET.name());

        assertEquals(functionSpec, result);
    }

    @Test
    void findFunctionSpec_missingTenant() {
        when(tenantContextHolder.getTenantKey()).thenReturn(TEST_TENANT);
        when(functionApiSpecConfiguration.getSpecByTenant(TEST_TENANT)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
            () -> functionService.findFunctionSpec("functionKey", GET.name()),
            "Function by key: key and tenant: tenant1 not found");
    }

    @Test
    void getValidFunctionInput_validJson() {
        FunctionApiSpec functionSpec = mock(FunctionApiSpec.class);
        Map<String, Object> functionInput = Map.of("key", "value");
        String validJsonSchema = "{\"type\":\"object\",\"properties\":{\"key\":{\"type\":\"string\"}},\"required\":[\"key\"]}";

        when(functionSpec.isValidateFunctionInput()).thenReturn(true);
        when(functionSpec.getInputSpec()).thenReturn(validJsonSchema);

        Map<String, Object> result = functionService.getValidFunctionInput(functionSpec, functionInput);

        assertEquals(functionInput, result);
    }

    @Test
    void getValidFunctionInput_invalidJson() {
        FunctionApiSpec functionSpec = mock(FunctionApiSpec.class);
        Map<String, Object> functionInput = Map.of("key", 123); // Invalid type
        String invalidJsonSchema = "{\"type\":\"object\",\"properties\":{\"key\":{\"type\":\"string\"}},\"required\":[\"key\"]}";

        when(functionSpec.isValidateFunctionInput()).thenReturn(true);
        when(functionSpec.getInputSpec()).thenReturn(invalidJsonSchema);

        assertThrows(JsonValidationUtils.InvalidJsonException.class,
            () -> functionService.getValidFunctionInput(functionSpec, functionInput),
            "key: expected type: String, found: Integer");
    }

    @Test
    void getValidFunctionInput_skipValidation() {
        FunctionApiSpec functionSpec = mock(FunctionApiSpec.class);
        Map<String, Object> functionInput = Map.of("key", 123); // Invalid type
        String invalidJsonSchema = "{\"type\":\"object\",\"properties\":{\"key\":{\"type\":\"string\"}},\"required\":[\"key\"]}";

        when(functionSpec.isValidateFunctionInput()).thenReturn(false);
        when(functionSpec.getInputSpec()).thenReturn(invalidJsonSchema);

        Map<String, Object> result = functionService.getValidFunctionInput(functionSpec, functionInput);

        assertEquals(functionInput, result);
    }

    @Test
    void getValidFunctionInput_inputNull() {
        FunctionApiSpec functionSpec = mock(FunctionApiSpec.class);

        when(functionSpec.isValidateFunctionInput()).thenReturn(false);

        Map<String, Object> result = functionService.getValidFunctionInput(functionSpec, null);

        assertEquals(0, result.size());
    }
}
