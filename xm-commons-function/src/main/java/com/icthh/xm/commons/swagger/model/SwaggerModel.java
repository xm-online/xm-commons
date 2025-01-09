package com.icthh.xm.commons.swagger.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.utils.Constants.SWAGGER_VERSION;

@Data
public class SwaggerModel implements Serializable {

    private final String openapi = SWAGGER_VERSION;

    private SwaggerInfo info = new SwaggerInfo();
    private List<ServerObject> servers = new ArrayList<>();
    private List<TagObject> tags = new ArrayList<>();
    private Map<String, Map<String, ApiMethod>> paths = new LinkedHashMap<>();
    private SwaggerComponents components = new SwaggerComponents();

    @JsonInclude()
    private List<Map<String, List<Object>>> security = new ArrayList<>();

    {
        security.add(Map.of("oAuth2Password", new ArrayList<>()));
        security.add(Map.of("oAuth2ClientCredentials", new ArrayList<>()));
    }
}
