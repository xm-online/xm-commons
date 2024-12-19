package com.icthh.xm.commons.processor.impl;

import com.icthh.xm.commons.domain.DefinitionSpec;
import com.icthh.xm.commons.domain.TestBaseSpecification;
import com.icthh.xm.commons.domain.TestSpecificationItem;
import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.commons.processor.ISpecProcessor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.icthh.xm.commons.utils.AssertionUtils.assertEqualsSpec;
import static com.icthh.xm.commons.utils.ReflectionUtils.getField;
import static com.icthh.xm.commons.utils.TestConstants.BASE_SPEC_KEY;
import static com.icthh.xm.commons.utils.TestConstants.TEST_TENANT;
import static com.icthh.xm.commons.utils.TestReadSpecUtils.loadBaseSpecByFileName;
import static com.icthh.xm.commons.utils.TestReadSpecUtils.loadJsonSpecFileByName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DefinitionSpecProcessorUnitTest {

    private JsonListenerService jsonListenerService = new JsonListenerService();
    private ISpecProcessor<DefinitionSpec> definitionSpecProcessor;

    @BeforeAll
    void setUp() {
        jsonListenerService = new JsonListenerService();
        definitionSpecProcessor = new DefinitionSpecProcessor(jsonListenerService);
        setUpJsonListenerService();
    }

    private void setUpJsonListenerService() {
        jsonListenerService.processTenantSpecification(TEST_TENANT, "json-definitions/address.json", loadJsonSpecFileByName("address"));
        jsonListenerService.processTenantSpecification(TEST_TENANT, "json-definitions/geoAddress.json", loadJsonSpecFileByName("geoAddress"));
        jsonListenerService.processTenantSpecification(TEST_TENANT, "json-definitions/userInfo.json", loadJsonSpecFileByName("userInfo"));
        jsonListenerService.processTenantSpecification(TEST_TENANT, "json-definitions/employee.json", loadJsonSpecFileByName("employee"));
    }

    @Test
    void updateStateByTenant_filterNullSpecifications() {
        definitionSpecProcessor.updateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, Set.of());
        assertTrue(getDefinitionsByTenantMap().isEmpty());

        definitionSpecProcessor.updateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, null);
        assertTrue(getDefinitionsByTenantMap().isEmpty());

        List<DefinitionSpec> list = new ArrayList<>();
        list.add(null);
        definitionSpecProcessor.updateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, list);
        assertTrue(getDefinitionsByTenantMap().isEmpty());
    }

    @Test
    void updateStateByTenant() {
        List<DefinitionSpec> definitions = loadBaseSpecByFileName("test-spec-simple").getDefinitions();

        definitionSpecProcessor.updateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, definitions);

        DefinitionSpec userInfoSpec = getSpecByKey(definitions, "userInfo");
        DefinitionSpec employeeInfoSpec = getSpecByKey(definitions, "employeeInfo");
        DefinitionSpec addressSpec = getSpecByKey(definitions, "address");

        var updatedDefinitionsMap = getDefinitionsByTenantMap();

        updatedDefinitionsMap.get(TEST_TENANT).get(BASE_SPEC_KEY).size();
        assertTrue(updatedDefinitionsMap.containsKey(TEST_TENANT));
        assertTrue(updatedDefinitionsMap.get(TEST_TENANT).containsKey(BASE_SPEC_KEY));
        assertEquals(3, updatedDefinitionsMap.get(TEST_TENANT).get(BASE_SPEC_KEY).size());
        assertEquals(userInfoSpec, updatedDefinitionsMap.get(TEST_TENANT).get(BASE_SPEC_KEY).get("userInfo"));
        assertEquals(employeeInfoSpec, updatedDefinitionsMap.get(TEST_TENANT).get(BASE_SPEC_KEY).get("employeeInfo"));
        assertEquals(addressSpec, updatedDefinitionsMap.get(TEST_TENANT).get(BASE_SPEC_KEY).get("address"));
    }

    @Test
    public void processDataSpec_simpleDefinition() {
        TestBaseSpecification expectedBaseSpec = loadBaseSpecByFileName("definitions/expected/simple-definition");
        TestBaseSpecification inputBaseSpec = loadBaseSpecByFileName("definitions/input/simple-definition");
        TestSpecificationItem itemSpec = getSpecItemByKey(inputBaseSpec, "BOOKING");

        definitionSpecProcessor.updateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, inputBaseSpec.getDefinitions());
        definitionSpecProcessor.processDataSpec(TEST_TENANT, BASE_SPEC_KEY, itemSpec::setInputDataSpec, itemSpec::getInputDataSpec);

        assertEqualsSpec(expectedBaseSpec, inputBaseSpec);
    }

    @Test
    public void processDataSpec_singleReference() {
        TestBaseSpecification expectedBaseSpec = loadBaseSpecByFileName("definitions/expected/single-ref-definition");
        TestBaseSpecification inputBaseSpec = loadBaseSpecByFileName("definitions/input/single-ref-definition");
        TestSpecificationItem itemSpec = getSpecItemByKey(inputBaseSpec, "team/CREATE_EMPLOYEE");

        definitionSpecProcessor.updateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, inputBaseSpec.getDefinitions());
        definitionSpecProcessor.processDataSpec(TEST_TENANT, BASE_SPEC_KEY, itemSpec::setInputDataSpec, itemSpec::getInputDataSpec);

        assertEqualsSpec(expectedBaseSpec, inputBaseSpec);
    }

    @Test
    public void processDataSpec_singleJsonReference() {
        TestBaseSpecification expectedBaseSpec = loadBaseSpecByFileName("definitions/expected/single-json-ref-definition");
        TestBaseSpecification inputBaseSpec = loadBaseSpecByFileName("definitions/input/single-json-ref-definition");
        TestSpecificationItem itemSpec = getSpecItemByKey(inputBaseSpec, "store/GET-FULL-ADDRESS");

        definitionSpecProcessor.updateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, inputBaseSpec.getDefinitions());
        definitionSpecProcessor.processDataSpec(TEST_TENANT, BASE_SPEC_KEY, itemSpec::setInputDataSpec, itemSpec::getInputDataSpec);

        assertEqualsSpec(expectedBaseSpec, inputBaseSpec);
    }

    @Test
    public void processDataSpec_recursiveJsonReference() {
        TestBaseSpecification expectedBaseSpec = loadBaseSpecByFileName("definitions/expected/recursive-json-ref-definition");
        TestBaseSpecification inputBaseSpec = loadBaseSpecByFileName("definitions/input/recursive-json-ref-definition");
        TestSpecificationItem itemSpec = getSpecItemByKey(inputBaseSpec, "store/GET-FULL-ADDRESS");

        definitionSpecProcessor.updateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, inputBaseSpec.getDefinitions());
        definitionSpecProcessor.processDataSpec(TEST_TENANT, BASE_SPEC_KEY, itemSpec::setInputDataSpec, itemSpec::getInputDataSpec);

        assertEqualsSpec(expectedBaseSpec, inputBaseSpec);
    }

    private TestSpecificationItem getSpecItemByKey(TestBaseSpecification spec, String key) {
        return spec.getItems().stream()
            .filter(d -> key.equals(d.getKey()))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("No TestSpecificationItem found with key: " + key));
    }

    private DefinitionSpec getSpecByKey(Collection<DefinitionSpec> definitions, String key) {
        return definitions.stream()
            .filter(d -> key.equals(d.getKey()))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("No DefinitionSpec found with key: " + key));
    }

    @SneakyThrows
    private Map<String, Map<String, Map<String, DefinitionSpec>>> getDefinitionsByTenantMap() {
        Field field = getField(definitionSpecProcessor, "definitionsByTenant");
        return (Map<String, Map<String, Map<String, DefinitionSpec>>>) field.get(definitionSpecProcessor);
    }
}
