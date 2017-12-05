package com.icthh.xm.commons.security.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

/**
 * The {@link SpringSecurityXmAuthenticationContextUnitTest} class.
 */
public class SpringSecurityXmAuthenticationContextUnitTest {

    @Test
    public void testGetLoginForOAuth2() {
        SecurityContext securityContext = mock(SecurityContext.class);
        OAuth2Authentication auth = mock(OAuth2Authentication.class);

        when((securityContext.getAuthentication())).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn("login");

        SpringSecurityXmAuthenticationContext xmAuthContext = new SpringSecurityXmAuthenticationContext(securityContext);
        assertEquals("login", xmAuthContext.getLogin().get());
    }

}
