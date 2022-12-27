package com.icthh.xm.commons.domainevent.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty("application.timeline-domain-event-enabled")
public class DomainEventConfig implements WebMvcConfigurer {

    private final WebApiSource webApiSource;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webApiSource);
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseSuffixPatternMatch(false);
    }

}
