package com.icthh.xm.commons.utils;

import java.util.HashSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.Error;
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
    private final SchemaRegistry factory = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

    public static Set<Error> validateJson(Map<String, Object> data, Schema schema) {
        Set<Error> errors = validate(data, schema);
        if (!errors.isEmpty()) {
            log.error("Validation data report: {}", getReportErrorMessage(errors));
        }
        return errors;
    }

    @SneakyThrows
    public static void assertJson(Map<String, Object> data, String jsonSchema) {
        Schema schema = factory.getSchema(jsonSchema);
        Set<Error> errors = validate(data, schema);
        if (!errors.isEmpty()) {
            String message = getReportErrorMessage(errors);
            log.error("Validation data report: {}", message);
            throw new InvalidJsonException(message);
        }
    }

    private String getReportErrorMessage(Set<Error> report) {
        return report.stream()
            .map(Error::getMessage)
            .collect(Collectors.joining(" | "));
    }

    @SneakyThrows
    private Set<Error> validate(Map<String, Object> data, Schema jsonSchema) {
        log.debug("Validation data. map: {}", data);
        JsonNode dataNode = objectMapper.valueToTree(data);
        return new HashSet<>(jsonSchema.validate(dataNode));
    }

    public static class InvalidJsonException extends BusinessException {
        public InvalidJsonException(String message) {
            super("error.validation.data.spec", message);
        }
    }
}
