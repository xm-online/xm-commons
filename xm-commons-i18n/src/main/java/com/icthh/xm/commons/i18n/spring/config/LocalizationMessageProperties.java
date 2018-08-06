package com.icthh.xm.commons.i18n.spring.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("xm-message-localization")
@Getter
@Setter
public class LocalizationMessageProperties {

    private final static String DEFAULT_CONFIG_PATH = "/config/tenants/{tenantName}/i18n-message.yml";

    private String configPath = DEFAULT_CONFIG_PATH;
}
