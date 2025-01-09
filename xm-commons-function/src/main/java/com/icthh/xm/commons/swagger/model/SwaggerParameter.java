package com.icthh.xm.commons.swagger.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class SwaggerParameter {

    private String in = "path";
    private String name;
    private Boolean required;
    private Map<String, Object> schema;

    public SwaggerParameter(String name, boolean required, Map<String, Object> schema) {
        this.name = name;
        this.required = required;
        this.schema = schema;
    }

    public SwaggerParameter(String in, String name, Boolean required, Map<String, Object> schema) {
        this(name, required, schema);
        this.in = in;
    }
}
