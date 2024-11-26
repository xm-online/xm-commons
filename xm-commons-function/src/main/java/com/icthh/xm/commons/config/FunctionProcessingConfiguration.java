package com.icthh.xm.commons.config;

import com.icthh.xm.commons.service.FunctionExecutorService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FunctionProcessingConfiguration {

    @Bean
    @ConditionalOnMissingBean(FunctionExecutorService.class)
    public FunctionExecutorService functionExecutorService() {
        return new FunctionExecutorService() {
        };
    }
}
