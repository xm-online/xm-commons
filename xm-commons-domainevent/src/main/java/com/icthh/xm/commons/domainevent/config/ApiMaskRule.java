package com.icthh.xm.commons.domainevent.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ApiMaskRule {

    private String endpointToMask;
    private List<String> httpMethod;
    private List<String> pathToMask;
    private String mask;
    private boolean maskRequest;
    private boolean maskResponse;
}
