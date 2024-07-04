package com.icthh.xm.commons.flow.rest.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.icthh.xm.commons.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class JsonValidationService {

    public static final String REGEX_EOL = "\n";

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();

    public ProcessingReport validateJson(Map<String, Object> data, JsonSchema schema) {
        ProcessingReport report = validate(data, schema);
        if (!report.isSuccess()) {
            log.error("Validation data report: {}", getReportErrorMessage(report));
        }
        return report;
    }

    @SneakyThrows
    public void assertJson(Map<String, Object> data, String jsonSchema) {
        JsonSchema schema = factory.getJsonSchema(JsonLoader.fromString(jsonSchema));
        ProcessingReport report = validate(data, schema);
        if (!report.isSuccess()) {
            String message = getReportErrorMessage(report);
            log.error("Validation data report: {}", message);
            throw new InvalidJsonException(message);
        }
    }

    private String getReportErrorMessage(ProcessingReport report) {
        return report.toString().replaceAll(REGEX_EOL, " | ");
    }

    @SneakyThrows
    private ProcessingReport validate(Map<String, Object> data, JsonSchema jsonSchema) {
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
