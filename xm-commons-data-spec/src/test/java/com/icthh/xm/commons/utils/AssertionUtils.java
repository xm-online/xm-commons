package com.icthh.xm.commons.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.domain.TestBaseSpecification;
import com.icthh.xm.commons.domain.TestSpecificationItem;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UtilityClass
public class AssertionUtils {

    @SneakyThrows
    public static void assertEqualsSpec(TestBaseSpecification expectedBaseSpec, TestBaseSpecification actualBaseSpec) {
        Map<String, TestSpecificationItem> actualItems = actualBaseSpec.getItems().stream()
            .collect(Collectors.toMap(TestSpecificationItem::getKey, d -> d));

        expectedBaseSpec.getItems().forEach(expected -> {
                assertTrue(actualItems.containsKey(expected.getKey()));
                assertJsonEquals(expected.getInputDataSpec(), actualItems.get(expected.getKey()).getInputDataSpec());
                assertJsonEquals(expected.getOutputDataSpec(), actualItems.get(expected.getKey()).getOutputDataSpec());
                assertJsonEquals(expected.getInputFormSpec(), actualItems.get(expected.getKey()).getInputFormSpec());
                assertJsonEquals(expected.getOutputFormSpec(), actualItems.get(expected.getKey()).getOutputFormSpec());
            }
        );
        assertEquals(expectedBaseSpec.getDefinitions(), actualBaseSpec.getDefinitions());
        assertEquals(expectedBaseSpec.getForms(), actualBaseSpec.getForms());
    }

    @SneakyThrows
    public static void assertJsonEquals(String expected, String actual) {
        ObjectMapper objectMapper = new ObjectMapper();
        assertFalse(expected == null && actual != null);
        assertFalse(expected != null && actual == null);
        if (expected != null) {
            assertEquals(objectMapper.readTree(expected), objectMapper.readTree(actual));
        }
    }
}
