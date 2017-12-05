package com.icthh.xm.commons.timeline.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
public class ApiMaskRule {

    private String endpointToMask;
    private List<String> httpMethod;
    private List<String> pathToMask;
    private String mask;
    private boolean maskRequest;
    private boolean maskResponse;
}
