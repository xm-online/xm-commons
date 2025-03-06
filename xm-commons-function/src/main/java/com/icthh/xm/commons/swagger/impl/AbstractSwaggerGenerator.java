package com.icthh.xm.commons.swagger.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.config.swagger.DynamicSwaggerConfiguration;
import com.icthh.xm.commons.swagger.JsonSchemaToSwaggerSchemaConverter;
import com.icthh.xm.commons.swagger.SwaggerGenerator;
import com.icthh.xm.commons.swagger.model.ApiMethod;
import com.icthh.xm.commons.swagger.model.BodyContent;
import com.icthh.xm.commons.swagger.model.ServerObject;
import com.icthh.xm.commons.swagger.model.SwaggerContent;
import com.icthh.xm.commons.swagger.model.SwaggerFunction;
import com.icthh.xm.commons.swagger.model.SwaggerModel;
import com.icthh.xm.commons.swagger.model.SwaggerParameter;
import com.icthh.xm.commons.swagger.model.SwaggerResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.icthh.xm.commons.utils.Constants.POST_URLENCODED;
import static com.icthh.xm.commons.utils.SwaggerGeneratorUtils.getPathVariables;
import static com.icthh.xm.commons.utils.SwaggerGeneratorUtils.getRequestErrorDefinition;
import static com.icthh.xm.commons.utils.SwaggerGeneratorUtils.getResponses;
import static com.icthh.xm.commons.utils.SwaggerGeneratorUtils.skipUnsupportedFields;
import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Slf4j
public abstract class AbstractSwaggerGenerator implements SwaggerGenerator {

    private static final Set<String> METHODS_WITH_PARAMS = Set.of(GET.name(), DELETE.name());

    private final SwaggerModel swaggerBody = new SwaggerModel();
    private final JsonSchemaToSwaggerSchemaConverter jsonSchemaConverter;
    private final Map<String, Object> definitions = new LinkedHashMap<>();
    private final Map<String, Object> originalDefinitions = new LinkedHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final DynamicSwaggerConfiguration configuration;

    public AbstractSwaggerGenerator(String baseUrl, DynamicSwaggerConfiguration configuration,
                                    JsonSchemaToSwaggerSchemaConverter jsonSchemaConverter) {
        this.configuration = configuration;
        this.jsonSchemaConverter = jsonSchemaConverter;
        this.swaggerBody.setServers(List.of(new ServerObject(baseUrl)));
        ofNullable(configuration).map(DynamicSwaggerConfiguration::getInfo).ifPresent(this.swaggerBody::setInfo);
        ofNullable(configuration).map(DynamicSwaggerConfiguration::getTags).ifPresent(this.swaggerBody::setTags);
        ofNullable(configuration).map(DynamicSwaggerConfiguration::getServers).ifPresent(this.swaggerBody::setServers);
        this.swaggerBody.getComponents().setSchemas(definitions);
        this.swaggerBody.getComponents().getResponses().putAll(getResponses());
        this.definitions.put("RequestError", getRequestErrorDefinition());
    }

    public abstract void enrichApiMethod(ApiMethod operation, Map<String, SwaggerParameter> pathPrefixParams,
                                         SwaggerFunction swaggerFunction, String httpMethod);
    public abstract ObjectNode generateFunctionContext();

    @Override
    public SwaggerModel getSwaggerBody() {
        return this.swaggerBody;
    }

    @Override
    public void generateFunction(String pathPrefix, Map<String, SwaggerParameter> pathPrefixParams, SwaggerFunction swaggerFunction) {
        Map<String, Map<String, ApiMethod>> paths = swaggerBody.getPaths();
        Map<String, ApiMethod> methods = new HashMap<>();
        swaggerFunction.getHttpMethods().stream()
            .filter(m -> !m.equalsIgnoreCase(POST_URLENCODED) || !swaggerFunction.getHttpMethods().contains(POST.name()))
            .forEach(httpMethod -> {
                ApiMethod apiMethod = generateApiMethod(pathPrefixParams, swaggerFunction, httpMethod);
                httpMethod = httpMethod.equalsIgnoreCase(POST_URLENCODED) ? POST.name() : httpMethod;
                methods.put(httpMethod.toLowerCase(), apiMethod);
            });

        paths.computeIfAbsent(pathPrefix + swaggerFunction.getPath(), k -> new HashMap<>()).putAll(methods);
    }

    private ApiMethod generateApiMethod(Map<String, SwaggerParameter> pathPrefixParams,
                                        SwaggerFunction swaggerFunction,
                                        String httpMethod) {
        ApiMethod operation = new ApiMethod();

        operation.setOperationId(swaggerFunction.getOperationId(), swaggerFunction.hasMultipleHttpMethods(), httpMethod);
        buildParameters(pathPrefixParams, swaggerFunction, operation, httpMethod);
        operation.setSummary(swaggerFunction.getName());
        operation.setDescription(swaggerFunction.getDescription());
        operation.setTags(swaggerFunction.getTags());
        buildResponse(swaggerFunction, operation, httpMethod);
        if (TRUE.equals(swaggerFunction.getAnonymous())) {
            operation.setSecurity(List.of());
        }
        enrichApiMethod(operation, pathPrefixParams, swaggerFunction, httpMethod);
        return operation;
    }

    private void buildResponse(SwaggerFunction swaggerFunction, ApiMethod operation, String httpMethod) {
        operation.setResponses(new LinkedHashMap<>());
        operation.generateDefaultResponses();
        if (POST.name().equals(httpMethod) || POST_URLENCODED.equals(httpMethod)) {
            addSuccess(swaggerFunction, operation, "201");
        } else {
            addSuccess(swaggerFunction, operation, "200");
        }
    }

    private void addSuccess(SwaggerFunction swaggerFunction, ApiMethod operation, String code) {
        String successfulMessage = "Successful operation";

        JsonNode jsonNode = jsonSchemaConverter.transformToJsonNode(
            operation.getOperationId(),
            swaggerFunction.getOutputJsonSchema(),
            definitions,
            originalDefinitions
        );
        if (TRUE.equals(swaggerFunction.getWrapResult())) {
            ObjectNode functionContext = generateFunctionContext();
            ObjectNode properties = (ObjectNode) functionContext.get("properties");
            if (isNotBlank(swaggerFunction.getOutputJsonSchema())) {
                properties.set("data", jsonNode);
            } else {
                ObjectNode objectNode = objectMapper.createObjectNode();
                objectNode.put("type", "object");
                objectNode.put("additionalProperties", true);
                properties.set("data", objectNode);
            }
            jsonNode = functionContext;
        }
        Map<String, Object> schema = convertToMap(jsonNode);
        operation.getResponses().put(code, new SwaggerResponse(new BodyContent(new SwaggerContent(schema)), successfulMessage));
    }

    private void buildParameters(Map<String, SwaggerParameter> pathPrefixParams,
                                 SwaggerFunction swaggerFunction,
                                 ApiMethod operation,
                                 String httpMethod) {

        JsonNode jsonNode = jsonSchemaConverter.transformToJsonNode(
            operation.getOperationId(),
            swaggerFunction.getInputJsonSchema(),
            definitions,
            originalDefinitions
        );
        jsonSchemaConverter.inlineRootRef(jsonNode, definitions);
        Map<String, SwaggerParameter> parameters = new LinkedHashMap<>(pathPrefixParams);
        addPathParameters(swaggerFunction.getPath(), jsonNode, parameters);
        addQueryParameters(jsonNode, parameters, httpMethod);
        if (isNotBlank(swaggerFunction.getInputJsonSchema())) {
            Map<String, Object> schema = convertToMap(jsonNode);
            operation.setRequestBody(swaggerFunction.getHttpMethods(), schema, httpMethod);
        }
        operation.setParameters(new ArrayList<>(parameters.values()));
    }

    private Map<String, Object> convertToMap(JsonNode jsonNode) {
        return objectMapper.convertValue(jsonNode, new TypeReference<>() {});
    }

    private void addQueryParameters(JsonNode jsonNode, Map<String, SwaggerParameter> parameters, String httpMethod) {
        if (METHODS_WITH_PARAMS.contains(httpMethod)) {
            if (jsonNode.isObject() && jsonNode.has("properties")) {
                ObjectNode object = (ObjectNode) jsonNode.get("properties");
                var fields = object.fields();
                Set<String> requiredFields = getRequiredFieldsFromFromSchema(jsonNode);

                while (fields.hasNext()) {
                    var field = fields.next();
                    if (skipUnsupportedFields(field)) {
                        continue;
                    }

                    Map<String, Object> schema = convertToMap(field.getValue());
                    // return true for empty list for backward compatibility, could be changed later
                    boolean required = requiredFields.isEmpty() || requiredFields.contains(field.getKey());
                    parameters.put(field.getKey(), new SwaggerParameter("query", field.getKey(), required, schema));
                    fields.remove();
                }
            }
        }
    }

    private Set<String> getRequiredFieldsFromFromSchema(JsonNode jsonNode) {
        if (!jsonNode.has("required") || !jsonNode.get("required").isArray() || jsonNode.get("required").isEmpty()) {
            return Set.of();
        }
        Set<String> requiredFields = new HashSet<>();
        for (JsonNode item : jsonNode.get("required")) {
            requiredFields.add(item.textValue());
        }
        return requiredFields;
    }

    private void addPathParameters(String swaggerFunctionPath, JsonNode jsonNode, Map<String, SwaggerParameter> parameters) {
        List<String> variablesInPath = getPathVariables(swaggerFunctionPath);
        variablesInPath.forEach(variable -> {
            if (jsonNode.has("properties") && jsonNode.get("properties").has(variable)) {
                ObjectNode object = (ObjectNode) jsonNode.get("properties");
                JsonNode variableSchema = object.remove(variable);
                Map<String, Object> schema = convertToMap(variableSchema);
                parameters.put(variable, new SwaggerParameter(variable, true, schema));
            } else {
                parameters.put(variable, new SwaggerParameter(variable, true, Map.of("type", "string")));
            }
        });
    }
}
