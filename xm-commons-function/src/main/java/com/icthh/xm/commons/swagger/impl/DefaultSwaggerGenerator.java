package com.icthh.xm.commons.swagger.impl;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import com.icthh.xm.commons.config.swagger.DynamicSwaggerConfiguration;
import com.icthh.xm.commons.domain.DefaultFunctionResult;
import com.icthh.xm.commons.swagger.JsonSchemaToSwaggerSchemaConverter;
import com.icthh.xm.commons.swagger.model.ApiMethod;
import com.icthh.xm.commons.swagger.model.SwaggerFunction;
import com.icthh.xm.commons.swagger.model.SwaggerParameter;
import lombok.SneakyThrows;

import java.util.Map;
import tools.jackson.module.jsonSchema.JsonSchema;
import tools.jackson.module.jsonSchema.JsonSchemaGenerator;

public class DefaultSwaggerGenerator extends AbstractSwaggerGenerator {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public DefaultSwaggerGenerator(String baseUrl, DynamicSwaggerConfiguration configuration) {
        super(baseUrl, configuration, new JsonSchemaToSwaggerSchemaConverter());
    }

    @Override
    public void enrichApiMethod(ApiMethod operation, Map<String, SwaggerParameter> pathParams, SwaggerFunction function, String httpMethod) {
    }

    @SneakyThrows
    @Override
    public ObjectNode generateFunctionContext() {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(objectMapper);
        JsonSchema jsonSchema = schemaGen.generateSchema(DefaultFunctionResult.class);
        ObjectNode tree = (ObjectNode) objectMapper.readTree(objectMapper.writeValueAsString(jsonSchema));
        ObjectNode properties = (ObjectNode) tree.get("properties");
        tree.remove("id");
        properties.remove("modelAndView");
        return tree;
    }
}
