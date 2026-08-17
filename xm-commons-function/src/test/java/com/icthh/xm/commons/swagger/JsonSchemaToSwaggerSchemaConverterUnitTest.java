package com.icthh.xm.commons.swagger;

import com.icthh.xm.commons.exceptions.BusinessException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static com.icthh.xm.commons.tenant.JsonMapperUtils.getDefaultJsonMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers schemas where the {@code type} keyword is not a plain string node.
 * Jackson 3 {@code JsonNode.asString()} throws {@code JsonNodeException} on array and object nodes
 * (unlike Jackson 2 {@code asText()}, which returned an empty string), so every such schema used to
 * blow up with a low level mapping error instead of being converted or reported as a business error.
 */
public class JsonSchemaToSwaggerSchemaConverterUnitTest {

    private static final String TYPE_NAME = "Type";

    private final ObjectMapper objectMapper = getDefaultJsonMapper();
    private final JsonSchemaToSwaggerSchemaConverter converter = new JsonSchemaToSwaggerSchemaConverter();

    @Test
    public void convertUnionTypeWithNullToAnyOfAndNullable() {
        // language=json
        String jsonSchema = """
            {
              "type": "object",
              "properties": {
                "myField": {
                  "type": ["string", "integer", "null"]
                }
              }
            }
            """;

        // language=json
        String expected = """
            {
              "type": "object",
              "properties": {
                "myField": {
                  "nullable": true,
                  "anyOf": [{"type": "string"}, {"type": "integer"}]
                }
              }
            }
            """;

        assertConverted(jsonSchema, expected);
    }

    @Test
    public void convertUnionTypeWithNullOnRootLevel() {
        // language=json
        String jsonSchema = """
            {
              "type": ["string", "integer", "null"]
            }
            """;

        // language=json
        String expected = """
            {
              "nullable": true,
              "anyOf": [{"type": "string"}, {"type": "integer"}]
            }
            """;

        assertConverted(jsonSchema, expected);
    }

    @Test
    public void convertSingleTypeWithNullToNullableType() {
        // language=json
        String jsonSchema = """
            {
              "type": "object",
              "properties": {
                "myField": {
                  "type": ["string", "null"]
                }
              }
            }
            """;

        // language=json
        String expected = """
            {
              "type": "object",
              "properties": {
                "myField": {
                  "type": "string",
                  "nullable": true
                }
              }
            }
            """;

        assertConverted(jsonSchema, expected);
    }

    @Test
    public void convertNullOnlyTypeToNullableWithoutType() {
        // language=json
        String jsonSchema = """
            {
              "type": "object",
              "properties": {
                "myField": {
                  "type": ["null"]
                }
              }
            }
            """;

        // language=json
        String expected = """
            {
              "type": "object",
              "properties": {
                "myField": {
                  "nullable": true
                }
              }
            }
            """;

        assertConverted(jsonSchema, expected);
    }

    @Test
    public void convertNullableArrayTypeAddsItems() {
        // language=json
        String jsonSchema = """
            {
              "type": "object",
              "properties": {
                "myField": {
                  "type": ["array", "null"],
                  "items": {"type": "string"}
                }
              }
            }
            """;

        // language=json
        String expected = """
            {
              "type": "object",
              "properties": {
                "myField": {
                  "type": "array",
                  "nullable": true,
                  "items": {"type": "string"}
                }
              }
            }
            """;

        assertConverted(jsonSchema, expected);
    }

    @Test
    public void throwBusinessExceptionWhenTypeIsObject() {
        // language=json
        String jsonSchema = """
            {
              "type": "object",
              "properties": {
                "myField": {
                  "type": {"unexpected": "object"}
                }
              }
            }
            """;

        assertInvalidType(jsonSchema);
    }

    @Test
    public void throwBusinessExceptionWhenUnionTypeContainsObject() {
        // language=json
        String jsonSchema = """
            {
              "type": "object",
              "properties": {
                "myField": {
                  "type": ["string", {"unexpected": "object"}]
                }
              }
            }
            """;

        assertInvalidType(jsonSchema);
    }

    @Test
    public void ignoreTrailingContentAfterJsonSchema() {
        // A tenant misindents the next YAML key into the inputSpec block scalar, so the spec string
        // carries trailing YAML after the JSON object. Jackson 2 readTree ignored trailing tokens,
        // so such specs worked for years; Jackson 3 enables FAIL_ON_TRAILING_TOKENS by default.
        // language=json
        String jsonSchema = """
            {
              "type": "object",
              "properties": {
                "userId": {
                  "type": "string"
                }
              }
            }
            """;
        String jsonSchemaWithTrailingYaml = jsonSchema + """
            outputSpec: |
              {
                "type": "object"
              }
            """;

        String expected = converter.transformToSwaggerJson(TYPE_NAME, jsonSchema, Map.of(), Map.of());
        assertConverted(jsonSchemaWithTrailingYaml, expected);
    }

    private void assertConverted(String jsonSchema, String expected) {
        String actual = converter.transformToSwaggerJson(TYPE_NAME, jsonSchema, Map.of(), Map.of());
        assertEquals(objectMapper.readTree(expected), objectMapper.readTree(actual));
    }

    private void assertInvalidType(String jsonSchema) {
        BusinessException exception = assertThrows(BusinessException.class,
            () -> converter.transformToSwaggerJson(TYPE_NAME, jsonSchema, Map.of(), Map.of()));
        assertEquals("error.invalid.json.type", exception.getCode());
    }
}
