package com.icthh.xm.commons.swagger.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class RequestBody {

    private Boolean required;
    private Object content;

    public RequestBody(Boolean required, BodyContent content) {
        this.content = content;
        this.required = required;
    }

    public RequestBody(Boolean required, Map<String, SwaggerContent> content) {
        this.content = content;
        this.required = required;
    }
}
