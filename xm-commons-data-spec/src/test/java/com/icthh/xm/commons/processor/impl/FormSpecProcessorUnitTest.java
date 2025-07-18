package com.icthh.xm.commons.processor.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.icthh.xm.commons.domain.DataSpec;
import com.icthh.xm.commons.domain.DefinitionSpec;
import com.icthh.xm.commons.domain.FormSpec;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FormSpecProcessorUnitTest {

    private JsonListenerService jsonListenerService = new JsonListenerService();
    private ISpecProcessor<FormSpec> formSpecProcessor;

    @BeforeEach
    void setUp() {
        jsonListenerService = new JsonListenerService();
        formSpecProcessor = new FormSpecProcessor(jsonListenerService);
        setUpJsonListenerService();
    }

    private void setUpJsonListenerService() {
        addJsonToListener(jsonListenerService, "json-forms/employeeForm");
        addJsonToListener(jsonListenerService, "json-forms/itemsByCategoryForm");
        addJsonToListener(jsonListenerService, "json-forms/testKey");
        addJsonToListener(jsonListenerService, "json-forms/userDetails");
    }

    @Test
    void updateStateByTenant_filterNullSpecifications() {
        formSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, Set.of());
        assertTrue(getFormsByTenantMap().isEmpty());

        formSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, null);
        assertTrue(getFormsByTenantMap().isEmpty());

        List<FormSpec> list = new ArrayList<>();
        list.add(null);
        formSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, list);
        assertTrue(getFormsByTenantMap().isEmpty());
    }

    @Test
    void updateStateByTenant() {
        List<FormSpec> forms = loadBaseSpecByFileName("test-spec-simple").getForms();

        formSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, forms);

        DataSpec itemsByStoreForm = getSpecByKey(forms, "itemsByStoreForm");
        DataSpec itemByCategoryForm = getSpecByKey(forms, "itemByCategoryForm");
        DataSpec datesForm = getSpecByKey(forms, "datesForm");

        var updatedDefinitionsMap = getFormsByTenantMap();

        updatedDefinitionsMap.get(TEST_TENANT).get(BASE_SPEC_KEY).size();
        assertTrue(updatedDefinitionsMap.containsKey(TEST_TENANT));
        assertTrue(updatedDefinitionsMap.get(TEST_TENANT).containsKey(BASE_SPEC_KEY));
        assertEquals(3, updatedDefinitionsMap.get(TEST_TENANT).get(BASE_SPEC_KEY).size());
        assertEquals(itemsByStoreForm, updatedDefinitionsMap.get(TEST_TENANT).get(BASE_SPEC_KEY).get("itemsByStoreForm"));
        assertEquals(itemByCategoryForm, updatedDefinitionsMap.get(TEST_TENANT).get(BASE_SPEC_KEY).get("itemByCategoryForm"));
        assertEquals(datesForm, updatedDefinitionsMap.get(TEST_TENANT).get(BASE_SPEC_KEY).get("datesForm"));
    }

    @Test
    public void processDataSpec_simpleForm() {
        TestBaseSpecification expectedBaseSpec = loadBaseSpecByFileName("forms/expected/simple-form");
        TestBaseSpecification inputBaseSpec = loadBaseSpecByFileName("forms/input/simple-form");
        TestSpecificationItem itemSpec = getSpecItemByKey(inputBaseSpec, "CATEGORY");

        formSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, inputBaseSpec.getForms());
        formSpecProcessor.processDataSpec(TEST_TENANT, BASE_SPEC_KEY, itemSpec::setInputFormSpec, itemSpec::getInputFormSpec);

        assertEqualsSpec(expectedBaseSpec, inputBaseSpec);
    }

    @Test
    public void processDataSpec_singleReference() {
        TestBaseSpecification expectedBaseSpec = loadBaseSpecByFileName("forms/expected/single-ref-form");
        TestBaseSpecification inputBaseSpec = loadBaseSpecByFileName("forms/input/single-ref-form");
        TestSpecificationItem itemSpec = getSpecItemByKey(inputBaseSpec, "team/CREATE_EMPLOYEE");

        formSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, inputBaseSpec.getForms());
        formSpecProcessor.processDataSpec(TEST_TENANT, BASE_SPEC_KEY, itemSpec::setInputFormSpec, itemSpec::getInputFormSpec);

        assertEqualsSpec(expectedBaseSpec, inputBaseSpec);
    }

    @Test
    public void processDataSpec_singleJsonReference() {
        TestBaseSpecification expectedBaseSpec = loadBaseSpecByFileName("forms/expected/single-json-ref-form");
        TestBaseSpecification inputBaseSpec = loadBaseSpecByFileName("forms/input/single-json-ref-form");
        TestSpecificationItem itemSpec = getSpecItemByKey(inputBaseSpec, "team/CREATE_EMPLOYEE");

        formSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, inputBaseSpec.getForms());
        formSpecProcessor.processDataSpec(TEST_TENANT, BASE_SPEC_KEY, itemSpec::setInputFormSpec, itemSpec::getInputFormSpec);

        assertEqualsSpec(expectedBaseSpec, inputBaseSpec);
    }

    @Test
    public void processDataSpec_recursiveJsonReference() {
        TestBaseSpecification expectedBaseSpec = loadBaseSpecByFileName("forms/expected/recursive-json-ref-form");
        TestBaseSpecification inputBaseSpec = loadBaseSpecByFileName("forms/input/recursive-json-ref-form");
        TestSpecificationItem itemSpec = getSpecItemByKey(inputBaseSpec, "item/SEARCH-ITEMS-BY-CATEGORY");

        formSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, inputBaseSpec.getForms());
        formSpecProcessor.processDataSpec(TEST_TENANT, BASE_SPEC_KEY, itemSpec::setInputFormSpec, itemSpec::getInputFormSpec);

        assertEqualsSpec(expectedBaseSpec, inputBaseSpec);
    }

    @Test
    public void processDataSpec_multipleFormsWithPrefixes() {
        TestBaseSpecification expectedBaseSpec = loadBaseSpecByFileName("forms/expected/multiple-form");
        TestBaseSpecification inputBaseSpec = loadBaseSpecByFileName("forms/input/multiple-form");
        TestSpecificationItem itemSpec = getSpecItemByKey(inputBaseSpec, "MULTIPLE");

        formSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, inputBaseSpec.getForms());
        formSpecProcessor.processDataSpec(TEST_TENANT, BASE_SPEC_KEY, itemSpec::setInputFormSpec, itemSpec::getInputFormSpec);

        assertEqualsSpec(expectedBaseSpec, inputBaseSpec);
    }

    @Test
    public void processDataSpec_invalidJson() {
        TestBaseSpecification inputBaseSpec = loadBaseSpecByFileName("forms/input/single-json-ref-form");
        TestSpecificationItem itemSpec = getSpecItemByKey(inputBaseSpec, "team/CREATE_EMPLOYEE");

        // override json
        jsonListenerService.processTenantSpecification(TEST_TENANT,"json-forms/employeeForm.json", "{,}");
        formSpecProcessor.fullUpdateStateByTenant(TEST_TENANT, BASE_SPEC_KEY, inputBaseSpec.getForms());

        assertThrows(JsonProcessingException.class, () ->
            formSpecProcessor.processDataSpec(TEST_TENANT, BASE_SPEC_KEY, itemSpec::setInputFormSpec, itemSpec::getInputFormSpec)
        );
    }

    @SneakyThrows
    private Map<String, Map<String, Map<String, DefinitionSpec>>> getFormsByTenantMap() {
        Field field = getField(formSpecProcessor, "formsByTenant");
        return (Map<String, Map<String, Map<String, DefinitionSpec>>>) field.get(formSpecProcessor);
    }
}
