package com.icthh.xm.commons.swagger.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.icthh.xm.commons.utils.Constants.POST_URLENCODED;
import static java.lang.String.join;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;
import static org.apache.commons.text.CaseUtils.toCamelCase;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

@Data
public class ApiMethod {

    private static final Set<String> METHODS_WITH_BODY = Set.of(POST.name(), PUT.name(), PATCH.name(), POST_URLENCODED);

    private String summary;
    private String description;
    private String operationId;
    private List<String> tags = new ArrayList<>();
    private List<SwaggerParameter> parameters = new ArrayList<>();
    private Object requestBody;
    private Map<String, SwaggerResponse> responses = new LinkedHashMap<>();
    @JsonInclude(NON_NULL)
    private List<Object> security;

    public void setOperationId(String operationId, boolean hasMultipleHttpMethods, String httpMethod) {
        if (hasMultipleHttpMethods) {
            operationId = operationId + httpMethod;
        }
        operationId = join("-", splitByCharacterTypeCamelCase(operationId));
        operationId = toCamelCase(operationId, false, '_', '-', '/', ' ');
        this.operationId = operationId;
    }

    public void generateDefaultResponses() {
        this.responses.put("400", new SwaggerResponse("#/components/responses/400"));
        this.responses.put("401", new SwaggerResponse("#/components/responses/401"));
        this.responses.put("403", new SwaggerResponse("#/components/responses/403"));
        this.responses.put("404", new SwaggerResponse("#/components/responses/404"));
        this.responses.put("500", new SwaggerResponse("#/components/responses/500"));
    }

    public void setRequestBody(List<String> httpMethods, Map<String, Object> schema, String httpMethod) {
        if (!METHODS_WITH_BODY.contains(httpMethod)) {
            return;
        }
        if (httpMethod.equals(POST.name()) && httpMethods.contains(POST_URLENCODED)) {
            this.requestBody = new RequestBody(true, Map.of(
                "application/x-www-form-urlencoded", new SwaggerContent(schema),
                "application/json", new SwaggerContent(schema)
            ));
        } else if (httpMethod.equals(POST_URLENCODED) && !httpMethods.contains(POST.name())) {
            this.requestBody = new RequestBody(true, Map.of(
                "application/x-www-form-urlencoded", new SwaggerContent(schema)
            ));
        } else if (!httpMethod.equals(POST_URLENCODED)) {
            this.requestBody = new RequestBody(true, new BodyContent(new SwaggerContent(schema)));
        }
    }
}
