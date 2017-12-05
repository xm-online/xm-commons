package com.icthh.xm.commons.web.spring.config;

import com.icthh.xm.commons.logging.LogstashConfigurer;
import com.icthh.xm.commons.logging.LogstashConfigurer.XmLogstashConfig;
import io.github.jhipster.config.JHipsterProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogstashConfiguration {

    public LogstashConfiguration(@Value("${spring.application.name}") String appName,
                                 @Value("${server.port}") Integer appPort,
                                 @Value("${spring.cloud.consul.discovery.instanceId}") String instanceId,
                                 JHipsterProperties jhipsterProperties) {

        JHipsterProperties.Logging.Logstash logstash = jhipsterProperties.getLogging().getLogstash();

        XmLogstashConfig config = new XmLogstashConfig(appName,
                                                       appPort,
                                                       instanceId,
                                                       logstash.getHost(),
                                                       logstash.getPort(),
                                                       logstash.getQueueSize());
        LogstashConfigurer.initLogstash(config);
    }

}
