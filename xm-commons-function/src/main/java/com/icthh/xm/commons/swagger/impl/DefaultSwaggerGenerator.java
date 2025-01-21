package com.icthh.xm.commons.swagger.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.icthh.xm.commons.config.swagger.DynamicSwaggerConfiguration;
import com.icthh.xm.commons.domain.DefaultFunctionResult;
import com.icthh.xm.commons.swagger.JsonSchemaToSwaggerSchemaConverter;
import com.icthh.xm.commons.swagger.model.ApiMethod;
import com.icthh.xm.commons.swagger.model.SwaggerFunction;
import com.icthh.xm.commons.swagger.model.SwaggerParameter;
import lombok.SneakyThrows;

import java.util.Map;

public class DefaultSwaggerGenerator extends AbstractSwaggerGenerator {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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
