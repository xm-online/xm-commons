package com.icthh.xm.commons.swagger.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class SwaggerResponse {

    private String description;
    private Object content;
    @JsonProperty("$ref")
    private String $ref;

    public SwaggerResponse(BodyContent content) {
        this.content = content;
    }

    public SwaggerResponse(Map<String, SwaggerContent> content, String description) {
        this.content = content;
        this.description = description;
    }

    public SwaggerResponse(BodyContent content, String description) {
        this.description = description;
        this.content = content;
    }

    public SwaggerResponse(String $ref) {
        this.$ref = $ref;
    }
}
