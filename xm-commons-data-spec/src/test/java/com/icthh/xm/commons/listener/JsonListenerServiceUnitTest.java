package com.icthh.xm.commons.listener;

import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static com.icthh.xm.commons.utils.ReflectionUtils.getField;
import static com.icthh.xm.commons.utils.TestConstants.TEST_TENANT;
import static com.icthh.xm.commons.utils.TestReadSpecUtils.loadJsonSpecFileByName;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

public class JsonListenerServiceUnitTest {

    private final JsonListenerService jsonListenerService = spy(new JsonListenerService());

    @Test
    void processTenantSpecification_addNewSpecification() {
        String relativePath = "/json-definitions/address.json";
        String config = loadJsonSpecFileByName("json-definitions/address");

        jsonListenerService.processTenantSpecification(TEST_TENANT, relativePath, config);

        Map<String, Map<String, String>> updatedMap = getUpdatedMap();

        assertNotNull(updatedMap);
        assertTrue(updatedMap.containsKey(TEST_TENANT));
        assertTrue(updatedMap.get(TEST_TENANT).containsKey(relativePath));
        assertEquals(config, updatedMap.get(TEST_TENANT).get(relativePath));
    }

    @Test
    void processTenantSpecification_updateSpecification() {
        String relativePath = "/json-definitions/address.json";

        // add spec
        jsonListenerService.processTenantSpecification(TEST_TENANT, relativePath, loadJsonSpecFileByName("json-definitions/address"));

        // update spec
        String config2 = loadJsonSpecFileByName("json-definitions/userInfo");
        jsonListenerService.processTenantSpecification(TEST_TENANT, relativePath, config2);

        // assert
        Map<String, Map<String, String>> updatedMap = getUpdatedMap();

        assertNotNull(updatedMap);
        assertTrue(updatedMap.containsKey(TEST_TENANT));
        assertTrue(updatedMap.get(TEST_TENANT).containsKey(relativePath));
        assertEquals(config2, updatedMap.get(TEST_TENANT).get(relativePath));
    }

    @Test
    void processTenantSpecification_removeSpecification_whenConfigIsBlank() {
        String relativePath = "/json-definitions/address.json";
        String config = loadJsonSpecFileByName("json-definitions/address");

        // add spec
        jsonListenerService.processTenantSpecification(TEST_TENANT, relativePath, config);

        // remove spec
        jsonListenerService.processTenantSpecification(TEST_TENANT, relativePath, EMPTY);

        // assert
        Map<String, Map<String, String>> updatedMap = getUpdatedMap();

        assertNotNull(updatedMap);
        assertTrue(updatedMap.containsKey(TEST_TENANT));
        assertTrue(updatedMap.get(TEST_TENANT).isEmpty());
    }

    @Test
    void getSpecificationByTenantRelativePath_existingPath() {
        String relativePath = "/definitions/userInfo.json";
        String config = loadJsonSpecFileByName("json-definitions/userInfo");
        jsonListenerService.processTenantSpecification(TEST_TENANT, relativePath, config);

        assertEquals(config, jsonListenerService.getSpecificationByTenantRelativePath(TEST_TENANT, relativePath));
    }

    @Test
    void getSpecificationByTenantRelativePath_nonExistingPath() {
        String relativePath = "/definitions/userInfo.json";
        String config = loadJsonSpecFileByName("json-definitions/userInfo");
        jsonListenerService.processTenantSpecification(TEST_TENANT, relativePath, config);
        String nonExistingRelativePath = RandomStringUtils.randomAlphanumeric(15);

        assertEquals(EMPTY, jsonListenerService.getSpecificationByTenantRelativePath(TEST_TENANT, nonExistingRelativePath));
    }

    @Test
    void getSpecificationByTenantRelativePath_nullRelativePath() {
        assertEquals(EMPTY, jsonListenerService.getSpecificationByTenantRelativePath(TEST_TENANT, null));
    }

    @Test
    void getSpecificationByTenant_nonExistingTenant() {
        String relativePath = "/json-definitions/address.json";
        String config = loadJsonSpecFileByName("json-definitions/address");
        jsonListenerService.processTenantSpecification(TEST_TENANT, relativePath, config);

        Map<String, String> result = jsonListenerService.getSpecificationByTenant(TEST_TENANT);

        assertNotNull(result);
        assertTrue(result.containsKey(relativePath));
        assertEquals(config, result.get(relativePath));
    }

    @Test
    void getSpecificationByTenant_nullRelativePath() {
        jsonListenerService.processTenantSpecification(TEST_TENANT, null, null);
        Map<String, String> result = jsonListenerService.getSpecificationByTenant(TEST_TENANT);

        assertNull(result);
    }

    @SneakyThrows
    private Map<String, Map<String, String>> getUpdatedMap() {
        Field field = getField(jsonListenerService, "tenantsSpecificationsByPath");
        return  (Map<String, Map<String, String>>) field.get(jsonListenerService);
    }
}
