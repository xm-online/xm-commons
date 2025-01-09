package com.icthh.xm.commons.swagger.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class SwaggerComponents {

    private Map<String, Object> responses = new LinkedHashMap<>();
    private Map<String, Object> schemas = new LinkedHashMap<>();
    private Map<String, SecuritySchemes> securitySchemes = new LinkedHashMap<>();

    {
        securitySchemes.put("oAuth2Password", new SecuritySchemes("password"));
        securitySchemes.put("oAuth2ClientCredentials", new SecuritySchemes("clientCredentials"));
    }
}
