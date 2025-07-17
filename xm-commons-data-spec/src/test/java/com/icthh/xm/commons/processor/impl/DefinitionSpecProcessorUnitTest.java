package com.icthh.xm.commons.processor.impl;

import com.icthh.xm.commons.domain.DataSpec;
import com.icthh.xm.commons.domain.DefinitionSpec;
import com.icthh.xm.commons.domain.TestBaseSpecification;
import com.icthh.xm.commons.domain.TestSpecificationItem;
import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.commons.processor.ISpecProcessor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.icthh.xm.commons.utils.AssertionUtils.assertEqualsSpec;
import static com.icthh.xm.commons.utils.SpecUtils.addJsonToListener;
import static com.icthh.xm.commons.utils.SpecUtils.getSpecByKey;
import static com.icthh.xm.commons.utils.SpecUtils.getSpecItemByKey;
import static com.icthh.xm.commons.utils.ReflectionUtils.getField;
import static com.icthh.xm.commons.utils.TestConstants.BASE_SPEC_KEY;
import static com.icthh.xm.commons.utils.TestConstants.TEST_TENANT;
import static com.icthh.xm.commons.utils.TestReadSpecUtils.loadBaseSpecByFileName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefinitionSpecProcessorUnitTest {

    private JsonListenerService jsonListenerService = new JsonListenerService();
    private ISpecProcessor<DefinitionSpec> definitionSpecProcessor;

    @BeforeEach
    void setUp() {
        jsonListenerService = new JsonListenerService();
        definitionSpecProcessor = new DefinitionSpecProcessor(jsonListenerService);
        setUpJsonListenerService();
    }

    private void setUpJsonListenerService() {
        addJsonToListener(jsonListenerService, "json-definitions/address");
        addJsonToListener(jsonListenerService, "json-definitions/geoAddress");
        addJsonToListener(jsonListenerService, "json-definitions/userInfo");
        addJsonToListener(jsonListenerService, "json-definitions/employee");
    }

    @Test
    void updateStateByTenant_filterNullSpecifications() {
        definitionSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, Set.of());
        assertTrue(getDefinitionsByTenantMap().isEmpty());

        definitionSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, null);
        assertTrue(getDefinitionsByTenantMap().isEmpty());

        List<DefinitionSpec> list = new ArrayList<>();
        list.add(null);
        definitionSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, list);
        assertTrue(getDefinitionsByTenantMap().isEmpty());
    }

    @Test
    void updateStateByTenant() {
        List<DefinitionSpec> definitions = loadBaseSpecByFileName("test-spec-simple").getDefinitions();

        definitionSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, definitions);

        DataSpec userInfoSpec = getSpecByKey(definitions, "userInfo");
        DataSpec employeeInfoSpec = getSpecByKey(definitions, "employeeInfo");
        DataSpec addressSpec = getSpecByKey(definitions, "address");

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
    void fullUpdateStateByTenant_When_RemoveDefinitionAnd_ExpectedCorrectSizeDefinitions() {
        TestBaseSpecification expectedBaseSpec = loadBaseSpecByFileName("definitions/expected/multiple-definition");
        TestBaseSpecification inputBaseSpec = loadBaseSpecByFileName("definitions/input/multiple-definition");
        TestSpecificationItem itemSpec = getSpecItemByKey(inputBaseSpec, "store/GET-EMPLOYEES-AGE");

        definitionSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, inputBaseSpec.getDefinitions());
        definitionSpecProcessor.processDataSpec(TEST_TENANT, BASE_SPEC_KEY, itemSpec::setInputDataSpec, itemSpec::getInputDataSpec);

        var updatedDefinitionsMap = getDefinitionsByTenantMap();
        assertEquals(4, updatedDefinitionsMap.get(TEST_TENANT).get(BASE_SPEC_KEY).size());

        assertEqualsSpec(expectedBaseSpec, inputBaseSpec);

        inputBaseSpec.getDefinitions().removeLast(); // remove last definition
        definitionSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, inputBaseSpec.getDefinitions());
        definitionSpecProcessor.processDataSpec(TEST_TENANT, BASE_SPEC_KEY, itemSpec::setInputDataSpec, itemSpec::getInputDataSpec);

        var secondaryUpdatedDefinitionsMap = getDefinitionsByTenantMap();
        assertEquals(3, secondaryUpdatedDefinitionsMap.get(TEST_TENANT).get(BASE_SPEC_KEY).size());
    }

    @Test
    public void processDataSpec_simpleDefinition() {
        TestBaseSpecification expectedBaseSpec = loadBaseSpecByFileName("definitions/expected/simple-definition");
        TestBaseSpecification inputBaseSpec = loadBaseSpecByFileName("definitions/input/simple-definition");
        TestSpecificationItem itemSpec = getSpecItemByKey(inputBaseSpec, "BOOKING");

        definitionSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, inputBaseSpec.getDefinitions());
        definitionSpecProcessor.processDataSpec(TEST_TENANT, BASE_SPEC_KEY, itemSpec::setInputDataSpec, itemSpec::getInputDataSpec);

        assertEqualsSpec(expectedBaseSpec, inputBaseSpec);
    }

    @Test
    public void processDataSpec_singleReference() {
        TestBaseSpecification expectedBaseSpec = loadBaseSpecByFileName("definitions/expected/single-ref-definition");
        TestBaseSpecification inputBaseSpec = loadBaseSpecByFileName("definitions/input/single-ref-definition");
        TestSpecificationItem itemSpec = getSpecItemByKey(inputBaseSpec, "team/CREATE_EMPLOYEE");

        definitionSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, inputBaseSpec.getDefinitions());
        definitionSpecProcessor.processDataSpec(TEST_TENANT, BASE_SPEC_KEY, itemSpec::setInputDataSpec, itemSpec::getInputDataSpec);

        assertEqualsSpec(expectedBaseSpec, inputBaseSpec);
    }

    @Test
    public void processDataSpec_singleJsonReference() {
        TestBaseSpecification expectedBaseSpec = loadBaseSpecByFileName("definitions/expected/single-json-ref-definition");
        TestBaseSpecification inputBaseSpec = loadBaseSpecByFileName("definitions/input/single-json-ref-definition");
        TestSpecificationItem itemSpec = getSpecItemByKey(inputBaseSpec, "store/GET-GEO-ADDRESS");

        definitionSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, inputBaseSpec.getDefinitions());
        definitionSpecProcessor.processDataSpec(TEST_TENANT, BASE_SPEC_KEY, itemSpec::setInputDataSpec, itemSpec::getInputDataSpec);

        assertEqualsSpec(expectedBaseSpec, inputBaseSpec);
    }

    @Test
    public void processDataSpec_recursiveJsonReference() {
        TestBaseSpecification expectedBaseSpec = loadBaseSpecByFileName("definitions/expected/recursive-json-ref-definition");
        TestBaseSpecification inputBaseSpec = loadBaseSpecByFileName("definitions/input/recursive-json-ref-definition");
        TestSpecificationItem itemSpec = getSpecItemByKey(inputBaseSpec, "store/GET-FULL-ADDRESS");

        definitionSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, inputBaseSpec.getDefinitions());
        definitionSpecProcessor.processDataSpec(TEST_TENANT, BASE_SPEC_KEY, itemSpec::setInputDataSpec, itemSpec::getInputDataSpec);

        assertEqualsSpec(expectedBaseSpec, inputBaseSpec);
    }

    @Test
    public void processDataSpec_multipleDefinitions() {
        TestBaseSpecification expectedBaseSpec = loadBaseSpecByFileName("definitions/expected/multiple-definition");
        TestBaseSpecification inputBaseSpec = loadBaseSpecByFileName("definitions/input/multiple-definition");
        TestSpecificationItem itemSpec = getSpecItemByKey(inputBaseSpec, "store/GET-EMPLOYEES-AGE");

        definitionSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, inputBaseSpec.getDefinitions());
        definitionSpecProcessor.processDataSpec(TEST_TENANT, BASE_SPEC_KEY, itemSpec::setInputDataSpec, itemSpec::getInputDataSpec);

        assertEqualsSpec(expectedBaseSpec, inputBaseSpec);
    }

    @Test
    public void processDataSpec_rootRefDefinitions() {
        TestBaseSpecification expectedBaseSpec = loadBaseSpecByFileName("definitions/expected/root-ref-definition");
        TestBaseSpecification inputBaseSpec = loadBaseSpecByFileName("definitions/input/root-ref-definition");
        TestSpecificationItem itemSpec = getSpecItemByKey(inputBaseSpec, "user/GET-INFO");

        definitionSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, inputBaseSpec.getDefinitions());
        definitionSpecProcessor.processDataSpec(TEST_TENANT, BASE_SPEC_KEY, itemSpec::setOutputDataSpec, itemSpec::getOutputDataSpec);

        assertEqualsSpec(expectedBaseSpec, inputBaseSpec);
    }

    @SneakyThrows
    private Map<String, Map<String, Map<String, DefinitionSpec>>> getDefinitionsByTenantMap() {
        Field field = getField(definitionSpecProcessor, "definitionsByTenant");
        return (Map<String, Map<String, Map<String, DefinitionSpec>>>) field.get(definitionSpecProcessor);
    }
}
