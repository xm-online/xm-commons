package com.icthh.xm.commons.domainevent.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnProperty(value = "application.domain-event.enabled", havingValue = "true")
public class DomainEventConfig implements WebMvcConfigurer {

    private final WebApiSource webApiSource;

    public DomainEventConfig(@Lazy WebApiSource webApiSource) {
        this.webApiSource = webApiSource;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webApiSource);
    }
}
