package com.icthh.xm.commons.lep.config;

import com.icthh.xm.commons.lep.spring.web.LepInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LepInterceptorConfiguration implements WebMvcConfigurer {

    private final LepInterceptor lepInterceptor;

    public LepInterceptorConfiguration(@Lazy LepInterceptor lepInterceptor) {
        this.lepInterceptor = lepInterceptor;
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(lepInterceptor).addPathPatterns("/**");
    }
}
