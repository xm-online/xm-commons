package com.icthh.xm.commons.flow.rest.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.icthh.xm.commons.flow.domain.TenantResource;
import com.icthh.xm.commons.flow.spec.resource.TenantResourceType;
import com.icthh.xm.commons.flow.spec.resource.ResourceTypeService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Map;

import static com.github.fge.jackson.JsonLoader.fromString;
import static com.github.fge.jsonschema.core.report.LogLevel.ERROR;
import static com.icthh.xm.commons.flow.rest.validator.JsonValidationService.REGEX_EOL;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.collections.MapUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@RequiredArgsConstructor
public class JsonDataValidator implements ConstraintValidator<JsonData, TenantResource> {

    private final ResourceTypeService resourceTypeService;
    private final ObjectMapper objectMapper;
    private final JsonValidationService jsonValidationService;

    @Override
    public void initialize(JsonData constraintAnnotation) {
        log.trace("Json data validator inited");
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    @SneakyThrows
    public boolean isValid(TenantResource value, ConstraintValidatorContext context) {
        TenantResourceType typeSpecification = resourceTypeService.getResource(value.getResourceType());
        if (!present(typeSpecification) || dataAndSpecificationEmpty(value.getData(), typeSpecification.getConfigSpec())) {
            return true;
        }

        if (dataWithoutSpecification(value.getData(), typeSpecification.getConfigSpec())) {
            log.error("Data specification null, but data is not null: {}", value.getData());
            return false;
        }

        var jsonSchema = JsonSchemaFactory.byDefault().getJsonSchema(fromString(typeSpecification.getConfigSpec()));
        return validate(value, jsonSchema, context);
    }

    private static boolean present(Object object) {
        return object != null;
    }

    private static boolean dataWithoutSpecification(Map<String, Object> value, String jsonSchema) {
        return isBlank(jsonSchema) && !isEmpty(value);
    }

    private static boolean dataAndSpecificationEmpty(Map<String, Object> value, String jsonSchema) {
        return isEmpty(value) && isBlank(jsonSchema);
    }

    @SneakyThrows
    private boolean validate(TenantResource value, JsonSchema jsonSchema, ConstraintValidatorContext context) {

        final ProcessingReport report = jsonValidationService.validateJson(value.getData(), jsonSchema);
        boolean isSuccess = report.isSuccess();
        if (!isSuccess) {
            log.error("Validation data report for resource with key {} and type {}: {}",
                    value.getKey(), value.getResourceType(), report.toString().replaceAll(REGEX_EOL, " | "));
            context.disableDefaultConstraintViolation();

            List<?> message = stream(report.spliterator(), false)
                .filter(error -> error.getLogLevel().equals(ERROR)).map(ProcessingMessage::asJson).collect(toList());
            context.buildConstraintViolationWithTemplate(objectMapper.writeValueAsString(message))
                   .addConstraintViolation();
        }
        return isSuccess;
    }

}
