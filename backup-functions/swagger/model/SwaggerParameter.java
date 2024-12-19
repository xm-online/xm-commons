package com.icthh.xm.commons.swagger.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
