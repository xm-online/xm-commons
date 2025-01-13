package com.icthh.xm.commons.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.icthh.xm.commons.domain.spec.IFunctionSpec;
import com.icthh.xm.commons.swagger.model.BodyContent;
import com.icthh.xm.commons.swagger.model.SwaggerContent;
import com.icthh.xm.commons.swagger.model.SwaggerResponse;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.icthh.xm.commons.swagger.JsonSchemaToSwaggerSchemaConverter.COMPONENTS_SCHEMAS;
import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.stripEnd;
import static org.apache.commons.lang3.StringUtils.stripStart;

/**
 * Utility class for dynamic swagger configuration
 */
@Slf4j
@UtilityClass
public class SwaggerGeneratorUtils {

    public static final Set<String> SUPPORTED_ANONYMOUS_HTTP_METHODS = Set.of("GET", "POST", "POST_URLENCODED");
    public static final Set<String> SUPPORTED_HTTP_METHODS = Set.of(
        "GET", "POST", "PUT", "DELETE", "PATCH", "POST_URLENCODED"
    );

    @NotNull
    public static List<String> getPathVariables(String swaggerFunctionPath) {
        List<String> variablesInPath = new ArrayList<>();
        new StringSubstitutor(key -> {
            variablesInPath.add(key);
            return key;
        }, "{", "}", '\\')
            .replace(swaggerFunctionPath);
        return variablesInPath;
    }

    public static boolean skipUnsupportedFields(Map.Entry<String, JsonNode> field) {
        if (field.getKey().equals("$ref")) {
            log.warn("$ref in query parameters is not supported");
            return true;
        }

        if (field.getValue().has("type") && field.getValue().get("type").asText().equals("object")) {
            log.warn("Object in query parameters is not supported");
            return true;
        }

        if (field.getValue().has("type") && field.getValue().get("type").asText().equals("array")) {
            JsonNode items = field.getValue().get("items");
            if (items.has("type") && items.get("type").asText().equals("object")) {
                log.warn("Array of objects in query parameters is not supported");
                return true;
            }

            if (items.has("$ref")) {
                log.warn("Array of $ref in query parameters is not supported");
                return true;
            }
        }
        return false;
    }

    public static Map<String, SwaggerResponse> getResponses() {
        Map<String, SwaggerResponse> responses = new HashMap<>();
        responses.put("400", getResponse("Bad request. Request invalid by business rules"));
        responses.put("401", getResponse("Invalid access token"));
        responses.put("403", getResponse("Forbidden"));
        responses.put("404", getResponse("Not found"));
        responses.put("500", getResponse("Internal server error"));
        return responses;
    }

    private SwaggerResponse getResponse(String description) {
        return new SwaggerResponse(
            new BodyContent(new SwaggerContent(Map.of("$ref", COMPONENTS_SCHEMAS + "RequestError"))),
            description
        );
    }

    public static Map<String, Object> getRequestErrorDefinition() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "error", Map.of(
                    "type", "string"
                ),
                "error_description", Map.of(
                    "type", "string"
                )
            )
        );
    }

    /**
     * This method prepares path for swagger api path
     * @param path  path to prepare
     * @return updated path
     */
    public static String makeAsPath(String path, String key) {
        String result = isNotBlank(path) ? path : key;
        result = stripStart(result, "/");
        result = stripEnd(result, "/");
        result = "/" + result;
        return result;
    }

    public static List<HttpMethodFilter> getSupportedHttpMethodFilters() {
        return new ArrayList<>(List.of(
            new HttpMethodFilter(
                fs -> TRUE.equals(fs.getAnonymous()),
                methods -> filterHttpMethods(methods, SUPPORTED_ANONYMOUS_HTTP_METHODS)
            ),
            new HttpMethodFilter(
                fs -> true,
                methods -> filterHttpMethods(methods, SUPPORTED_HTTP_METHODS)
            )
        ));
    }

    public static List<String> filterHttpMethods(List<String> httpMethods, Set<String> supportedMethods) {
        return httpMethods.stream().filter(supportedMethods::contains).collect(toList());
    }

    @AllArgsConstructor
    public class HttpMethodFilter {

        private final Predicate<IFunctionSpec> condition;
        private final Function<List<String>, List<String>> action;

        public boolean supported(IFunctionSpec functionSpec) {
            return condition.test(functionSpec);
        }

        public List<String> filter(List<String> httpMethods) {
            return action.apply(httpMethods);
        }
    }
}
