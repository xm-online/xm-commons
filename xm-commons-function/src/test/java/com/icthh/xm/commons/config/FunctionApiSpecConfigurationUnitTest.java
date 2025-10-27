package com.icthh.xm.commons.config;

import com.icthh.xm.commons.domain.spec.FunctionApiSpecs;
import com.icthh.xm.commons.domain.spec.FunctionSpec;
import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.commons.permission.service.custom.CustomPrivilegeSpecService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.commons.utils.Constants.FUNCTIONS;
import static com.icthh.xm.commons.utils.FunctionSpecReaderUtils.loadFunctionApiSpecByFile;
import static com.icthh.xm.commons.utils.FunctionUtils.countItems;
import static com.icthh.xm.commons.utils.FunctionUtils.findFunctionSpecByKey;
import static com.icthh.xm.commons.utils.TestConstants.APP_NAME;
import static com.icthh.xm.commons.utils.TestConstants.TENANT_KEY;
import static org.junit.jupiter.api.Assertions.*;

public class FunctionApiSpecConfigurationUnitTest {

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private JsonListenerService jsonListenerService;

    @Mock
    private FunctionApiSpecsProcessor functionApiSpecsProcessor;

    @Mock
    private CustomPrivilegeSpecService customPrivilegeSpecService;

    private FunctionApiSpecConfiguration functionApiSpecConfiguration;

    private FunctionApiSpecs specs1;
    private FunctionApiSpecs specs2;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.openMocks(this);
        functionApiSpecConfiguration = new FunctionApiSpecConfiguration(
            APP_NAME,
            jsonListenerService,
            customPrivilegeSpecService,
            tenantContextHolder,
            functionApiSpecsProcessor
        );
        setupTenantSpecification();
    }

    private void setupTenantSpecification() throws NoSuchFieldException, IllegalAccessException {
        Field specsByTenantField = functionApiSpecConfiguration.getClass()
            .getSuperclass()
            .getDeclaredField("specsByTenant");
        specsByTenantField.setAccessible(true);

        specs1 = loadFunctionApiSpecByFile("functions");
        specs2 = loadFunctionApiSpecByFile("functions-anonymous");
        var tenantSpecs = Map.of("specPath1", specs1, "specPath2", specs2);

        specsByTenantField.set(functionApiSpecConfiguration, Map.of(TENANT_KEY, tenantSpecs));
    }

    @Test
    void specKey() {
        assertEquals(FUNCTIONS, functionApiSpecConfiguration.specKey());
    }

    @Test
    void folder() {
        String expectedFolder = APP_NAME + "/" + FUNCTIONS;
        assertEquals(expectedFolder, functionApiSpecConfiguration.folder());
    }

    @Test
    void getSpecByKeyAndTenant_shouldReturnSpecWhenFound() {
        String expectedFunctionKey = "store/STORE-INFO";
        FunctionSpec expectedSpec = findFunctionSpecByKey(specs1, expectedFunctionKey);

        Optional<FunctionSpec> result = functionApiSpecConfiguration.getSpecByKeyAndTenant(expectedFunctionKey, TENANT_KEY);

        assertTrue(result.isPresent());
        assertEquals(expectedSpec, result.get());
    }

    @Test
    void getSpecByKeyAndTenant_shouldReturnEmptyWhenNotFound() {
        String missingFunctionKey = RandomStringUtils.randomAlphanumeric(8);

        Optional<FunctionSpec> result = functionApiSpecConfiguration.getSpecByKeyAndTenant(missingFunctionKey, TENANT_KEY);

        assertTrue(result.isEmpty());
    }

    @Test
    void getOrderedSpecByTenant_shouldReturnSortedSpecs() {
        Collection<FunctionSpec> result = functionApiSpecConfiguration.getOrderedSpecByTenant(TENANT_KEY);

        int expectedAmount = countItems(specs1) + countItems(specs2);
        List<FunctionSpec> expectedResult = List.of(
            findFunctionSpecByKey(specs2, "item/SEARCH-ITEMS-BY-STORE"),
            findFunctionSpecByKey(specs2, "item/SEARCH-ITEMS-BY-CATEGORY"),
            findFunctionSpecByKey(specs1, "store/GET-EMPLOYEES-AGE"),
            findFunctionSpecByKey(specs1, "store/STORE-INFO")
        );

        assertEquals(expectedAmount, result.size());
        assertEquals(expectedResult, result);
    }
}
