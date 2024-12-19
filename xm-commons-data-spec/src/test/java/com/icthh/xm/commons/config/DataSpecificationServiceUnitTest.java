package com.icthh.xm.commons.config;

import com.icthh.xm.commons.domain.TestBaseSpecification;
import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.commons.service.DefaultSpecProcessingService;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import static com.icthh.xm.commons.utils.ReflectionUtils.getSuperClassField;
import static com.icthh.xm.commons.utils.ReflectionUtils.setFieldValue;
import static com.icthh.xm.commons.utils.TestConstants.APP_NAME;
import static com.icthh.xm.commons.utils.TestConstants.BASE_SPEC_KEY;
import static com.icthh.xm.commons.utils.TestConstants.CONFIG_KEY_PREFIX;
import static com.icthh.xm.commons.utils.TestConstants.TEST_TENANT;
import static com.icthh.xm.commons.utils.TestReadSpecUtils.loadBaseSpecByFileName;
import static com.icthh.xm.commons.utils.TestReadSpecUtils.loadBaseSpecFileByName;
import static com.icthh.xm.commons.utils.TestReadSpecUtils.loadJsonSpecFileByName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class DataSpecificationServiceUnitTest {

    @Mock
    private JsonListenerService jsonListenerService;

    @Mock
    private DefaultSpecProcessingService<TestBaseSpecification> specProcessingService;

    private DataSpecificationService<TestBaseSpecification> dataSpecService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        var dataSpecService = new DataSpecificationService<>(TestBaseSpecification.class, jsonListenerService, specProcessingService) {
            @Override
            public String specKey() {
                return BASE_SPEC_KEY;
            }

            @Override
            public String folder() {
                return APP_NAME + "/" + BASE_SPEC_KEY;
            }
        };
        this.dataSpecService = spy(dataSpecService);
    }

    @Test
    void onRefresh_processYmlSpec() {
        String specFileName = "test-spec-simple";
        String updatedKey = CONFIG_KEY_PREFIX + ".yml";

        String specFile = loadBaseSpecFileByName(specFileName);
        TestBaseSpecification spec = loadBaseSpecByFileName(specFileName);

        Map<String, Map<String, String>> specFilesByTenant = getSpecFilesByTenantMap();

        assertTrue(dataSpecService.getTenantSpecifications(TEST_TENANT).isEmpty());
        assertTrue(getSpecFilesByTenantMap().isEmpty());

        dataSpecService.onRefresh(updatedKey, specFile);

        verify(specProcessingService).updateByTenantState(eq(TEST_TENANT), eq(BASE_SPEC_KEY), argThat(isSpecCollections(spec)));

        assertFalse(dataSpecService.getTenantSpecifications(TEST_TENANT).isEmpty());
        assertTrue(dataSpecService.getTenantSpecifications(TEST_TENANT).containsKey(updatedKey));
        assertEquals(spec, dataSpecService.getTenantSpecifications(TEST_TENANT).get(updatedKey));

        assertTrue(specFilesByTenant.containsKey(TEST_TENANT));
        assertTrue(specFilesByTenant.get(TEST_TENANT).containsKey(updatedKey));
        assertEquals(specFile, specFilesByTenant.get(TEST_TENANT).get(updatedKey));
    }

    @Test
    void onRefresh_YmlSpec_EmptyConfig() {
        String updatedKey = CONFIG_KEY_PREFIX + ".yml";

        dataSpecService.onRefresh(updatedKey, StringUtils.EMPTY);

        verifyNoInteractions(specProcessingService);

        assertTrue(dataSpecService.getTenantSpecifications(TEST_TENANT).isEmpty());
        assertTrue(getSpecFilesByTenantMap().isEmpty());
    }

    @Test
    void onRefresh_YmlSpec_CantReadTenantName() {
        String specFileName = "test-spec-simple";
        String invalidUpdatedKey = "/config/tenants/TENANT/appName/file.yml";
        String specFile = loadBaseSpecFileByName(specFileName);

        dataSpecService.onRefresh(invalidUpdatedKey, specFile);

        verifyNoInteractions(specProcessingService);

        assertTrue(dataSpecService.getTenantSpecifications(TEST_TENANT).isEmpty());
        assertTrue(getSpecFilesByTenantMap().isEmpty());
    }

    @SneakyThrows
    @Test
    void onRefresh_updateExistingYmlSpec() {
        String updatedKey = CONFIG_KEY_PREFIX + "/spec.yml";

        // set config to be updated
        String specFileName = "test-spec-simple";
        String specFile = loadBaseSpecFileByName(specFileName);
        TestBaseSpecification spec = loadBaseSpecByFileName(specFileName);

        setFieldValue(dataSpecService, "specsByTenant", Map.of(updatedKey, spec));
        setFieldValue(dataSpecService, "specFilesByTenant", Map.of(updatedKey, specFile));

        Map<String, Map<String, String>> specFilesByTenant = getSpecFilesByTenantMap();

        assertTrue(dataSpecService.getTenantSpecifications(TEST_TENANT).containsKey(updatedKey));
        assertEquals(spec, dataSpecService.getTenantSpecifications(TEST_TENANT).get(updatedKey));

        assertTrue(specFilesByTenant.get(TEST_TENANT).containsKey(updatedKey));
        assertEquals(specFile, specFilesByTenant.get(TEST_TENANT).get(updatedKey));

        // run updating existing spec
        String newSpecFileName = "test-spec-xm";
        String newSpecFile = loadBaseSpecFileByName(newSpecFileName);
        TestBaseSpecification newSpec = loadBaseSpecByFileName(newSpecFileName);

        dataSpecService.onRefresh(updatedKey, newSpecFile);

        // check spec was updated
        verify(specProcessingService).updateByTenantState(eq(TEST_TENANT), eq(BASE_SPEC_KEY), argThat(isSpecCollections(newSpec)));

        assertEquals(1, dataSpecService.getTenantSpecifications(TEST_TENANT).size());
        assertTrue(dataSpecService.getTenantSpecifications(TEST_TENANT).containsKey(updatedKey));
        assertEquals(newSpec, dataSpecService.getTenantSpecifications(TEST_TENANT).get(updatedKey));

        assertTrue(specFilesByTenant.containsKey(TEST_TENANT));
        assertTrue(specFilesByTenant.get(TEST_TENANT).containsKey(updatedKey));
        assertEquals(newSpecFile, specFilesByTenant.get(TEST_TENANT).get(updatedKey));
    }

    @Test
    void onRefresh_JsonSpec() {
        String specFileName = "json-definitions/address";
        String relativePath = "/json-definitions/address.json";
        String updatedKey = CONFIG_KEY_PREFIX + relativePath;

        String specFile = loadJsonSpecFileByName(specFileName);

        dataSpecService.onRefresh(updatedKey, specFile);

        verify(jsonListenerService).processTenantSpecification(eq(TEST_TENANT), eq(BASE_SPEC_KEY + relativePath), eq(specFile));
        verify(specProcessingService).updateByTenantState(eq(TEST_TENANT), eq(BASE_SPEC_KEY), argThat(isEmptySpecCollections()));
    }

    @Test
    void isListeningConfiguration() {
        assertFalse(dataSpecService.isListeningConfiguration("/config/tenants/TENANT/appName/file.yml"));
        assertFalse(dataSpecService.isListeningConfiguration("/config/tenants/TENANT/appName/test-spec.json"));
        assertFalse(dataSpecService.isListeningConfiguration("/config/tenants/appName/test-spec/file.yml"));
        assertFalse(dataSpecService.isListeningConfiguration("/config/tenants/TENANT/test-spec/file.yml"));
        assertFalse(dataSpecService.isListeningConfiguration("/config/tenants/TENANT/test-spec/file.json"));
        assertFalse(dataSpecService.isListeningConfiguration("/config/tenants/TENANT/test-spec/file.txt"));
        assertFalse(dataSpecService.isListeningConfiguration("/config/tenants/TENANT/appName/test-spec.yml"));
        assertFalse(dataSpecService.isListeningConfiguration("/config/tenants/TENANT/appTest/test-spec/dir/file.yml"));

        assertTrue(dataSpecService.isListeningConfiguration("/config/tenants/TENANT/appTest/test-spec.yml"));
        assertTrue(dataSpecService.isListeningConfiguration("/config/tenants/TENANT/appTest/test-spec/test-spec.yml"));
        assertTrue(dataSpecService.isListeningConfiguration("/config/tenants/TENANT/appTest/test-spec/any11.yml"));
        assertTrue(dataSpecService.isListeningConfiguration("/config/tenants/TENANT/appTest/test-spec/any11.json"));
        assertTrue(dataSpecService.isListeningConfiguration("/config/tenants/TENANT/appTest/test-spec/dir/dir/file.json"));
    }

    @SneakyThrows
    @Test
    void getTenantSpecifications() {
        // setup
        String updatedKey1 = "test-spec-simple";
        String specFileName1 = "test-spec-simple";
        String specFile1 = loadBaseSpecFileByName(specFileName1);
        TestBaseSpecification spec1 = loadBaseSpecByFileName(specFileName1);

        String updatedKey2 = "test-spec-xm";
        String specFileName2 = "test-spec-xm";
        String specFile2 = loadBaseSpecFileByName(specFileName2);
        TestBaseSpecification spec2 = loadBaseSpecByFileName(specFileName2);

        setFieldValue(dataSpecService, "specsByTenant", Map.of(updatedKey1, spec1, updatedKey2, spec2));
        setFieldValue(dataSpecService, "specFilesByTenant", Map.of(updatedKey1, specFile1, updatedKey2, specFile2));

        // validate
        Map<String, TestBaseSpecification> result = dataSpecService.getTenantSpecifications(TEST_TENANT);

        assertEquals(2, result.size());
        assertEquals(spec1, result.get(updatedKey1));
        assertEquals(spec2, result.get(updatedKey2));
    }

    private ArgumentMatcher<Collection<TestBaseSpecification>> isSpecCollections(TestBaseSpecification spec) {
        return actual -> actual != null && actual.size() == 1 && actual.contains(spec);
    }

    private ArgumentMatcher<Collection<TestBaseSpecification>> isEmptySpecCollections() {
        return actual -> actual != null && actual.isEmpty();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, String>> getSpecFilesByTenantMap() {
        try {
            Field field = getSuperClassField(dataSpecService, "specFilesByTenant");
            field.setAccessible(true);
            return (Map<String, Map<String, String>>) field.get(dataSpecService);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
