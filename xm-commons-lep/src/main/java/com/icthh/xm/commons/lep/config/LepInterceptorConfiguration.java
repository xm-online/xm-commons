package com.icthh.xm.commons.lep.config;

import com.icthh.xm.commons.lep.spring.web.LepInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@ConditionalOnMissingBean(LepInterceptorConfiguration.class)
public class LepInterceptorConfiguration implements WebMvcConfigurer {

    private final LepInterceptor lepInterceptor;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(lepInterceptor).addPathPatterns("/**");
    }
}
