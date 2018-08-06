package com.icthh.xm.commons.i18n.config;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;

/**
 * Mock configuration for {@link XmAuthenticationContextHolder}.
 */
public class MockXmAuthenticationContextConfiguration {

    @Bean
    public XmAuthenticationContextHolder authenticationContextHolder() {
        return Mockito.mock(XmAuthenticationContextHolder.class);
    }
}
