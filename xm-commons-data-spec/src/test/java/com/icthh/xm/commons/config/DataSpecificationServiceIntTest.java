package com.icthh.xm.commons.config;

import com.icthh.xm.commons.domain.DefinitionSpec;
import com.icthh.xm.commons.domain.TestBaseSpecification;
import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.commons.processor.impl.DefinitionSpecProcessor;
import com.icthh.xm.commons.processor.impl.FormSpecProcessor;
import com.icthh.xm.commons.service.DefaultSpecProcessingService;
import com.icthh.xm.commons.service.SpecificationProcessingService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.icthh.xm.commons.utils.AssertionUtils.assertEqualsSpec;
import static com.icthh.xm.commons.utils.TestConstants.APP_NAME;
import static com.icthh.xm.commons.utils.TestConstants.BASE_SPEC_KEY;
import static com.icthh.xm.commons.utils.TestConstants.CONFIG_KEY_PREFIX;
import static com.icthh.xm.commons.utils.TestConstants.TEST_TENANT;
import static com.icthh.xm.commons.utils.TestReadSpecUtils.loadBaseSpecByFileName;
import static com.icthh.xm.commons.utils.TestReadSpecUtils.loadBaseSpecFileByName;
import static com.icthh.xm.commons.utils.TestReadSpecUtils.loadJsonSpecFileByName;

public class DataSpecificationServiceIntTest {

    private DefinitionSpecProcessor definitionSpecProcessor;
    private FormSpecProcessor formSpecProcessor;
    private JsonListenerService jsonListenerService;
    private SpecificationProcessingService<TestBaseSpecification> specProcessingService;
    private DataSpecificationService<TestBaseSpecification> dataSpecService;

    @BeforeEach
    void setUp() {
        jsonListenerService = new JsonListenerService();
        definitionSpecProcessor = new DefinitionSpecProcessor(jsonListenerService);
        formSpecProcessor = new FormSpecProcessor(jsonListenerService);
        specProcessingService = new DefaultSpecProcessingService<>(definitionSpecProcessor, formSpecProcessor);

        dataSpecService = new DataSpecificationService<>(TestBaseSpecification.class, jsonListenerService, specProcessingService) {
            @Override
            public String specKey() {
                return BASE_SPEC_KEY;
            }

            @Override
            public String folder() {
                return APP_NAME + "/" + BASE_SPEC_KEY;
            }
        };
    }

    /**
     * This test is designed to check that specifications will be processed correctly despite the order
     * CONFIGS PROCESSING ORDER IS PART OF THE TEST !!!
     */
    @Test
    public void onRefresh_processSeveralFiles() {
        // process first json before config
        String dataFormJsonKey = CONFIG_KEY_PREFIX + "/json-forms/datesForm.json";
        String dataFormFileName = "json-forms/datesForm";
        dataSpecService.onRefresh(dataFormJsonKey, loadJsonSpecFileByName(dataFormFileName));

        // config has definition json reference, that are not processing yet
        String updatedKey = CONFIG_KEY_PREFIX + ".yml";
        String specFileName = "test-spec-simple";
        dataSpecService.onRefresh(updatedKey, loadBaseSpecFileByName(specFileName));

        // process second json after config
        String addressJsonKey = CONFIG_KEY_PREFIX + "/json-definitions/address.json";
        String addressFileName = "json-definitions/address";
        dataSpecService.onRefresh(addressJsonKey, loadJsonSpecFileByName(addressFileName));

        // config has 1 ref on definition from previous config yml & 1 ref on form from next processing config yml
        String updatedKey1 = CONFIG_KEY_PREFIX + "/test-spec-1.yml";
        String specFileName1 = "test-spec-1";
        dataSpecService.onRefresh(updatedKey1, loadBaseSpecFileByName(specFileName1));

        // process third json after config
        String balancesJsonKey = CONFIG_KEY_PREFIX + "/json-definitions/balances.json";
        String balancesFileName = "json-definitions/balances";
        dataSpecService.onRefresh(balancesJsonKey, loadJsonSpecFileByName(balancesFileName));

        // config has ref on form used in previous config
        String updatedKey2 = CONFIG_KEY_PREFIX + "/test-spec-2.yml";
        String specFileName2 = "test-spec-2";
        dataSpecService.onRefresh(updatedKey2, loadBaseSpecFileByName(specFileName2));

        // config with froms only
        String updatedKey3 = CONFIG_KEY_PREFIX + "/test-spec-3.yml";
        String specFileName3 = "test-spec-3";
        dataSpecService.onRefresh(updatedKey3, loadBaseSpecFileByName(specFileName3));

        assertEqualsTenantSpec(specFileName, updatedKey);
        assertEqualsTenantSpec(specFileName1, updatedKey1);
        assertEqualsTenantSpec(specFileName2, updatedKey2);
        assertEqualsTenantSpec(specFileName3, updatedKey3);
    }

    @Test
    public void onRefresh_processUpdateDefinitionSpec() {
        String specKeyPart2 = CONFIG_KEY_PREFIX + "/test-spec-simple-part-2.yml";
        String specKeyPart3 = CONFIG_KEY_PREFIX + "/test-spec-simple-part-3.yml";

        String specFilePart2Name = "test-spec-simple-part-2";
        String specFilePart3Name = "test-spec-simple-part-3";

        List<DefinitionSpec> definitionsPart2Before = loadBaseSpecByFileName(specFilePart2Name).getDefinitions();
        List<DefinitionSpec> definitionsPart3Before = loadBaseSpecByFileName(specFilePart3Name).getDefinitions();

        Assertions.assertEquals(3, definitionsPart2Before.size());
        Assertions.assertEquals(3, definitionsPart3Before.size());


        dataSpecService.onRefresh(specKeyPart2, loadBaseSpecFileByName(specFilePart2Name));
        dataSpecService.onRefresh(specKeyPart3, loadBaseSpecFileByName(specFilePart3Name));

        String specFilePart3NewContentName = "test-spec-simple-part-3-update";

        dataSpecService.onRefresh(specKeyPart3, loadBaseSpecFileByName(specFilePart3NewContentName));

        List<DefinitionSpec> definitionsPart2After = dataSpecService.getTenantSpecifications(TEST_TENANT).get(specKeyPart2).getDefinitions();
        List<DefinitionSpec> definitionsPart3After = dataSpecService.getTenantSpecifications(TEST_TENANT).get(specKeyPart3).getDefinitions();

        Assertions.assertEquals(definitionsPart2Before, definitionsPart2After);
        Assertions.assertEquals(2, definitionsPart3After.size());
    }

    private void assertEqualsTenantSpec(String expectedSpecRelativePath, String updatedKey) {
        TestBaseSpecification expected = loadBaseSpecByFileName("expected/" + expectedSpecRelativePath);
        TestBaseSpecification actual = dataSpecService.getTenantSpecifications(TEST_TENANT).get(updatedKey);
        assertEqualsSpec(expected, actual);
    }
}
