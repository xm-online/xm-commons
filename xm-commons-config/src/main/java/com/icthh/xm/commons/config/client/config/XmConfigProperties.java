package com.icthh.xm.commons.config.client.config;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("xm-config.enabled")
@ConfigurationProperties(prefix = "xm-config", ignoreUnknownFields = false)
@Data
public class XmConfigProperties {

    private Boolean enabled;
    private String xmConfigUrl;
    private String tenantConfigPattern;
    private String kafkaConfigTopic;
}
