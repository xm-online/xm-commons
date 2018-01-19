package com.icthh.xm.commons.request.spring.config;

import com.icthh.xm.commons.request.XmRequestContextHolder;
import com.icthh.xm.commons.request.internal.PrototypeXmRequestContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The {@link XmRequestContextConfiguration} class.
 */
@Configuration
public class XmRequestContextConfiguration {

    @Bean
    XmRequestContextHolder requestContextHolder() {
        return new PrototypeXmRequestContextHolder();
    }

}
