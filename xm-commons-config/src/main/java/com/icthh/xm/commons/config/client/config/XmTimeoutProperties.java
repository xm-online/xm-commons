package com.icthh.xm.commons.config.client.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "application.rest.timeouts")
@ConfigurationProperties(
    prefix = "application.rest.timeouts",
    ignoreUnknownFields = false
)
@Data
public class XmTimeoutProperties {

    private Duration readTimeout;
    private Duration connectionTimeout;
}
