package com.icthh.xm.commons.security.spring.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

@AutoConfiguration
public class SecurityExceptionHandlingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return new UnauthorizedEntryPoint();
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessDeniedHandler accessDeniedHandler() {
        return new ForbiddenAccessDeniedHandler();
    }
}
