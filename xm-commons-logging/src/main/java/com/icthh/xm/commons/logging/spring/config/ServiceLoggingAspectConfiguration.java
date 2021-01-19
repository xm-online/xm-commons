package com.icthh.xm.commons.logging.spring.config;

import com.icthh.xm.commons.logging.aop.ServiceLoggingAspect;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class ServiceLoggingAspectConfiguration {

    @Bean
    public ServiceLoggingAspect serviceLoggingAspect(LoggingConfigService loggingConfigService) {
        return new ServiceLoggingAspect(loggingConfigService);
    }

    @ConditionalOnMissingBean(LoggingConfigService.class)
    @Bean
    public LoggingConfigService loggingConfigService() {
        return new LoggingConfigServiceStub();
    }

}
