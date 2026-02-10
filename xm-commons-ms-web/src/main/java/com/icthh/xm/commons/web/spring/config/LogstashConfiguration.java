package com.icthh.xm.commons.web.spring.config;

import com.icthh.xm.commons.logging.LogstashConfigurer;
import com.icthh.xm.commons.logging.LogstashConfigurer.XmLogstashConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(LogstashProperties.class)
@ConditionalOnProperty(prefix = "application.logging.logstash", name = "enabled", havingValue = "true")
public class LogstashConfiguration {

    private final LogstashProperties logstashProperties;
    private final Environment env;

    @PostConstruct
    void initLogstash() {
        XmLogstashConfig config = new XmLogstashConfig(
            env.getProperty("spring.application.name"),
            env.getProperty("server.port", Integer.class, 5000),
            env.getProperty("spring.cloud.consul.discovery.instanceId", "512"),
            logstashProperties.getHost(),
            logstashProperties.getPort(),
            logstashProperties.getRingBufferSize()
        );
        LogstashConfigurer.initLogstash(config);
    }
}
