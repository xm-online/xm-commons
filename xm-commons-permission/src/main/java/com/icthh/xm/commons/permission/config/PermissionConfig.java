package com.icthh.xm.commons.permission.config;

import com.icthh.xm.commons.permission.service.PermissionEvaluationContextBuilder;
import com.icthh.xm.commons.permission.service.XmPermissionEvaluationContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PermissionConfig {

    @Bean
    public PermissionEvaluationContextBuilder permissionEvaluationContextBuilder() {
        return new XmPermissionEvaluationContextBuilder();
    }
}
