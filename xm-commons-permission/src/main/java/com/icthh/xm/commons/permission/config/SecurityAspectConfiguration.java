package com.icthh.xm.commons.permission.config;

import com.icthh.xm.commons.permission.aop.SecurityAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class SecurityAspectConfiguration {

    @Bean
    public SecurityAspect securityAspect() {
        return new SecurityAspect();
    }
}
