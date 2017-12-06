package com.icthh.xm.commons.security.spring.config;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.security.internal.SpringSecurityXmAuthenticationContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The {@link XmAuthenticationContextConfiguration} class.
 */
@Configuration
public class XmAuthenticationContextConfiguration {

    /**
     * XmAuthenticationContextHolder bean configure.
     * @return XmAuthenticationContextHolder bean instance
     */
    @Bean
    public XmAuthenticationContextHolder xmAuthenticationContextHolder() {
        return new SpringSecurityXmAuthenticationContextHolder();
    }

}
