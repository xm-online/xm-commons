package com.icthh.xm.commons.domainevent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "application.domain-event")
@Getter
@Setter
public class ApiMaskConfig {

    private List<ApiMaskRule> maskRules = new ArrayList<>();

}
