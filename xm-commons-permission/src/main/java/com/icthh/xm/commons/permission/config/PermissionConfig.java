package com.icthh.xm.commons.permission.config;

import com.icthh.xm.commons.permission.access.AbstractResourceFactory;
import com.icthh.xm.commons.permission.access.ResourceFactory;
import com.icthh.xm.commons.permission.access.XmPermissionEvaluator;
import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.commons.permission.service.PermissionEvaluationContextBuilder;
import com.icthh.xm.commons.permission.service.XmPermissionEvaluationContextBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;

import java.util.Map;

@Configuration
public class PermissionConfig {

    @Bean
    public PermissionEvaluationContextBuilder permissionEvaluationContextBuilder() {
        return new XmPermissionEvaluationContextBuilder();
    }

    @Primary
    @Bean
    static MethodSecurityExpressionHandler expressionHandler(XmPermissionEvaluator customPermissionEvaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(customPermissionEvaluator);
        return expressionHandler;
    }

    @Bean
    @ConditionalOnMissingBean(ResourceFactory.class)
    public ResourceFactory resourceFactory() {
        return new AbstractResourceFactory() {
            @Override
            protected Map<String, ? extends ResourceRepository<?, ?>> getRepositories() {
                return Map.of();
            }
        };
    }
}
