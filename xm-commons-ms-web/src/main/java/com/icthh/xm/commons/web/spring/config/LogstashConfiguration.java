package com.icthh.xm.commons.web.spring.config;

import com.icthh.xm.commons.logging.LogstashConfigurer;
import com.icthh.xm.commons.logging.LogstashConfigurer.XmLogstashConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogstashConfiguration {

    public LogstashConfiguration(@Value("${spring.application.name}") String appName,
                                 @Value("${server.port}") Integer appPort,
                                 @Value("${spring.cloud.consul.discovery.instanceId}") String instanceId,
                                 @Value("${application.logging.logstash.host}") String logstashHost,
                                 @Value("${application.logging.logstash.port}") int logstashPort,
                                 @Value("${application.logging.logstash.ring-buffer-size}") int ringBufferSize) {

        XmLogstashConfig config = new XmLogstashConfig(
            appName,
            appPort,
            instanceId,
            logstashHost,
            logstashPort,
            ringBufferSize);
        LogstashConfigurer.initLogstash(config);
    }

}
