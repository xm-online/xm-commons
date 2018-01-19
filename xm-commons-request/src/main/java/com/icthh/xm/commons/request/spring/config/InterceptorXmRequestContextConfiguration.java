package com.icthh.xm.commons.request.spring.config;

import com.icthh.xm.commons.request.XmRequestContextHolder;
import com.icthh.xm.commons.request.spring.XmRequestContextInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Objects;

/**
 * The {@link InterceptorXmRequestContextConfiguration} class.
 */
@Configuration
@Import(XmRequestContextConfiguration.class)
public abstract class InterceptorXmRequestContextConfiguration {

    private final String contextRequestSourceKey;
    private final Object requestSourceType;

    public InterceptorXmRequestContextConfiguration(String contextRequestSourceKey,
                                                    Object requestSourceType) {
        this.contextRequestSourceKey = Objects.requireNonNull(contextRequestSourceKey);
        this.requestSourceType = Objects.requireNonNull(requestSourceType);
    }

    @Bean
    XmRequestContextInterceptor xmRequestSourceInitInterceptor(XmRequestContextHolder requestContextHolder) {
        return new XmRequestContextInterceptor(requestContextHolder, contextRequestSourceKey, requestSourceType);
    }

}
