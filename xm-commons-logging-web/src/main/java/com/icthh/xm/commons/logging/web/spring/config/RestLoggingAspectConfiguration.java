package com.icthh.xm.commons.logging.web.spring.config;

import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.util.BasePackageDetector;
import com.icthh.xm.commons.logging.web.aop.RestCallLoggingAspect;
import com.icthh.xm.commons.logging.web.aop.RestLoggingAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class RestLoggingAspectConfiguration {

    @Bean
    @ConditionalOnProperty(value = "aspects.rest-logging", matchIfMissing = true)
    public RestLoggingAspect restLoggingAspect(LoggingConfigService loggingConfigService, BasePackageDetector basePackageDetector) {
        return new RestLoggingAspect(loggingConfigService, basePackageDetector);
    }

    @Bean
    @ConditionalOnProperty("aspects.rest-call-logging")
    public RestCallLoggingAspect restCallLoggingAspect() {
        return new RestCallLoggingAspect();
    }

}
