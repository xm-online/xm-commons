package com.icthh.xm.commons.swagger.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class SecuritySchemes {

    private String type;
    private Map<String, SecurityFlow> flows;

    public SecuritySchemes() {
        this.type = "oauth2";
        this.flows = new HashMap<>();
    }

    public SecuritySchemes(String flow) {
        this();
        this.flows.put(flow, new SecurityFlow());
    }
}
