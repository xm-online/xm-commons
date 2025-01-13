package com.icthh.xm.commons.swagger.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class SecurityFlow {

    private String tokenUrl;
    private Map<String, String> scopes;

    public SecurityFlow() {
        this.tokenUrl = "/uaa/oauth/token";
        this.scopes = new HashMap<>(Map.of("openapi", "Default client scope"));;
    }
}
