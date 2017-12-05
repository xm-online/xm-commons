package com.icthh.xm.commons.timeline.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "timelines", ignoreUnknownFields = false)
@Getter
@Setter
public class ApiMaskConfig {

    private List<ApiMaskRule> maskRules = new ArrayList<>();

}
