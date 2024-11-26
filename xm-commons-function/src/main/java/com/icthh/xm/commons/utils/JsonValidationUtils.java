package com.icthh.xm.commons.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class to validate json data
 */
@Slf4j
@UtilityClass
public class JsonValidationUtils {

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);

    public static Set<ValidationMessage> validateJson(Map<String, Object> data, JsonSchema schema) {
        Set<ValidationMessage> errors = validate(data, schema);
        if (!errors.isEmpty()) {
            log.error("Validation data report: {}", getReportErrorMessage(errors));
        }
        return errors;
    }

    @SneakyThrows
    public static void assertJson(Map<String, Object> data, String jsonSchema) {
        JsonSchema schema = factory.getSchema(jsonSchema);
        Set<ValidationMessage> errors = validate(data, schema);
        if (!errors.isEmpty()) {
            String message = getReportErrorMessage(errors);
            log.error("Validation data report: {}", message);
            throw new InvalidJsonException(message);
        }
    }

    private String getReportErrorMessage(Set<ValidationMessage> report) {
        return report.stream()
            .map(ValidationMessage::getMessage)
            .collect(Collectors.joining(" | "));
    }

    @SneakyThrows
    private Set<ValidationMessage> validate(Map<String, Object> data, JsonSchema jsonSchema) {
        log.debug("Validation data. map: {}", data);
        JsonNode dataNode = objectMapper.valueToTree(data);
        return jsonSchema.validate(dataNode);
    }

    public static class InvalidJsonException extends BusinessException {
        public InvalidJsonException(String message) {
            super("error.validation.data.spec", message);
        }
    }
}
