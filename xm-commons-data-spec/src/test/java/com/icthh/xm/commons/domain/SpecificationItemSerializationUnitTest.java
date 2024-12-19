package com.icthh.xm.commons.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpecificationItemSerializationUnitTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testMethodsAreIgnoredWhenSerializingToJson() throws JsonProcessingException {
        TestSpecificationItem item = new TestSpecificationItem();
        item.setInputSpec("input-spec");
        item.setInputForm("input-form");
        item.setOutputSpec("output-spec");
        item.setOutputForm("output-form");

        String json = objectMapper.writeValueAsString(item);

        assertTrue(json.contains("\"inputSpec\":\"input-spec\""));
        assertTrue(json.contains("\"inputForm\":\"input-form\""));
        assertTrue(json.contains("\"outputSpec\":\"output-spec\""));
        assertTrue(json.contains("\"outputForm\":\"output-form\""));

        assertFalse(json.contains("\"key\":null")); // missing as nullable
        assertFalse(json.contains("getInputDataSpec"));
        assertFalse(json.contains("getInputFormSpec"));
        assertFalse(json.contains("getOutputFormSpec"));
        assertFalse(json.contains("getOutputDataSpec"));
    }

    @Test
    void testDeserialization() throws JsonProcessingException {
        String json = """
        {
            "key": "test-key",
            "inputSpec": "input-spec",
            "inputForm": "input-form",
            "outputSpec": "output-spec",
            "outputForm": "output-form"
        }
        """;

        TestSpecificationItem item = objectMapper.readValue(json, TestSpecificationItem.class);

        assertEquals("test-key", item.getKey());
        assertEquals("input-spec", item.getInputDataSpec());
        assertEquals("input-form", item.getInputFormSpec());
        assertEquals("output-spec", item.getOutputDataSpec());
        assertEquals("output-form", item.getOutputFormSpec());
    }
}
