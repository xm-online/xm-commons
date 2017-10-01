package com.icthh.xm.commons.logging.config;

import com.icthh.xm.commons.logging.aop.RestCallLoggingAspect;
import com.icthh.xm.commons.logging.aop.RestLoggingAspect;
import com.icthh.xm.commons.logging.aop.ServiceLoggingAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class LoggingAspectConfiguration {

    @Bean
    public RestLoggingAspect restLoggingAspect() {
        return new RestLoggingAspect();
    }

    @Bean
    @ConditionalOnProperty("aspects.rest-call-logging")
    public RestCallLoggingAspect restCallLoggingAspect() {
        return new RestCallLoggingAspect();
    }

    @Bean
    public ServiceLoggingAspect serviceLoggingAspect() {
        return new ServiceLoggingAspect();
    }

}
