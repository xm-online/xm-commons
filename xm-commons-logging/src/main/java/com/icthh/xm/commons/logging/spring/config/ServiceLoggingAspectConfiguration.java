package com.icthh.xm.commons.logging.spring.config;

import com.icthh.xm.commons.logging.aop.ServiceLoggingAspect;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub;
import com.icthh.xm.commons.logging.util.BasePackageDetector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class ServiceLoggingAspectConfiguration {

    @Bean
    public ServiceLoggingAspect serviceLoggingAspect(LoggingConfigService loggingConfigService, BasePackageDetector basePackageDetector) {
        return new ServiceLoggingAspect(loggingConfigService, basePackageDetector);
    }

    @Bean
    @ConditionalOnMissingBean(BasePackageDetector.class)
    public BasePackageDetector basePackageDetector(ApplicationContext context) {
        return new BasePackageDetector(context);
    }

    @ConditionalOnMissingBean(LoggingConfigService.class)
    @Bean
    public LoggingConfigService loggingConfigService() {
        return new LoggingConfigServiceStub();
    }

}
