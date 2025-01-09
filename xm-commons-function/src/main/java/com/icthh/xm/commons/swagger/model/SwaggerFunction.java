package com.icthh.xm.commons.swagger.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SwaggerFunction {
    private String operationId;
    private String path;
    private String name;
    private String description;
    private String inputJsonSchema;
    private String outputJsonSchema;
    private List<String> tags;
    private List<String> httpMethods;
    private Boolean wrapResult;
    private Boolean anonymous;

    public boolean hasMultipleHttpMethods() {
        return this.httpMethods.size() > 1;
    }
}
