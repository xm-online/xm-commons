package com.icthh.xm.commons.web.spring.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "application.logging.logstash")
public class LogstashProperties {

    private String host;
    private int port;
    private int ringBufferSize;
}
