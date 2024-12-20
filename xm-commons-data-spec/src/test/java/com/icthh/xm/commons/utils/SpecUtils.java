package com.icthh.xm.commons.utils;

import com.icthh.xm.commons.domain.DataSpec;
import com.icthh.xm.commons.domain.TestBaseSpecification;
import com.icthh.xm.commons.domain.TestSpecificationItem;
import com.icthh.xm.commons.listener.JsonListenerService;
import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.icthh.xm.commons.utils.TestConstants.TEST_TENANT;
import static com.icthh.xm.commons.utils.TestReadSpecUtils.loadJsonSpecFileByName;

@UtilityClass
public class SpecUtils {

    public TestBaseSpecification findSpecByConfigKey(String configKey, Map<String, Map<String, TestBaseSpecification>> specs) {
        return specs.get(TEST_TENANT).get(configKey);
    }

    public static TestSpecificationItem getSpecItemByKey(TestBaseSpecification spec, String key) {
        return spec.getItems().stream()
            .filter(d -> key.equals(d.getKey()))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("No TestSpecificationItem found with key: " + key));
    }

    public static DataSpec getSpecByKey(Collection<? extends DataSpec> specs, String key) {
        return specs.stream()
            .filter(f -> key.equals(f.getKey()))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("No FormSpec found with key: " + key));
    }

    public static void addJsonToListener(JsonListenerService jsonListenerService, String relativePath) {
        jsonListenerService.processTenantSpecification(TEST_TENANT, relativePath + ".json", loadJsonSpecFileByName(relativePath));
    }
}
