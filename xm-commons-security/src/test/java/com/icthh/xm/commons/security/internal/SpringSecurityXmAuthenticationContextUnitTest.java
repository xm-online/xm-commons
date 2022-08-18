package com.icthh.xm.commons.security.internal;

import com.icthh.xm.commons.security.jwt.TokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.impl.FixedClock;
import io.jsonwebtoken.security.SignatureException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The {@link SpringSecurityXmAuthenticationContextUnitTest} class.
 */
@Slf4j
public class SpringSecurityXmAuthenticationContextUnitTest {

    @Test
    public void testGetLoginForOAuth2() {
        XmAuthentication auth = mock(XmAuthentication.class);
        when(auth.getPrincipal()).thenReturn("login");
        SpringSecurityXmAuthenticationContext xmAuthContext = authToXmAuthContext(auth);
        assertEquals("login", xmAuthContext.getLogin().get());
    }

    private SpringSecurityXmAuthenticationContext authToXmAuthContext(XmAuthentication auth) {
        SecurityContext securityContext = mock(SecurityContext.class);
        when((securityContext.getAuthentication())).thenReturn(auth);
        return new SpringSecurityXmAuthenticationContext(securityContext);
    }

    @Test
    public void testDecodeXmUserToken() {
        String token = loadFileString("test/mockUserToken");

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setRemoteAddr("mockRemoteAddr");
        MockHttpSession session = new MockHttpSession(null, "mockTestSessionId");
        mockHttpServletRequest.setSession(session);

        Date date = Date.from(LocalDate.of(2022, 07, 06).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        TokenProvider tokenProvider = new TokenProvider(() -> loadFile("test/public.cer"), new FixedClock(date));
        XmAuthentication authentication = tokenProvider.getAuthentication(mockHttpServletRequest, token);
        assertFalse(authentication.isClientOnly());
        var xmAuthContext = authToXmAuthContext(authentication);
        assertEquals("xm", xmAuthContext.getLogin().orElse(null));
        assertEquals("xm", xmAuthContext.getRequiredLogin());
        assertEquals("webapp", xmAuthContext.getRequiredClientId());
        assertEquals(Set.of("openid"), xmAuthContext.getScope());
        assertEquals("mockRemoteAddr", xmAuthContext.getRemoteAddress().orElse(null));
        assertEquals("mockTestSessionId", xmAuthContext.getSessionId().orElse(null));
        assertEquals(Set.of("SUPER-ADMIN"), xmAuthContext.getAuthoritiesSet());
        log.info(">> {}", authentication);
    }

    @Test
    public void testDecodeXmClientToken() {
        String token = loadFileString("test/mockClientToken");

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setRemoteAddr("mockRemoteAddr");
        MockHttpSession session = new MockHttpSession(null, "mockTestSessionId");
        mockHttpServletRequest.setSession(session);

        Date date = Date.from(LocalDate.of(2022, 07, 06).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        TokenProvider tokenProvider = new TokenProvider(() -> loadFile("test/public.cer"), new FixedClock(date));
        XmAuthentication authentication = tokenProvider.getAuthentication(mockHttpServletRequest, token);
        assertTrue(authentication.isClientOnly());
        var xmAuthContext = authToXmAuthContext(authentication);
        assertEquals("test", xmAuthContext.getLogin().orElse(null));
        assertEquals("test", xmAuthContext.getRequiredLogin());
        assertEquals("test", xmAuthContext.getRequiredClientId());
        assertEquals(Set.of("openid", "scope1", "test_scope2"), xmAuthContext.getScope());
        assertEquals("mockRemoteAddr", xmAuthContext.getRemoteAddress().orElse(null));
        assertEquals("mockTestSessionId", xmAuthContext.getSessionId().orElse(null));
        assertEquals(Set.of("ROLE_VIEW"), xmAuthContext.getAuthoritiesSet());
        log.info(">> {}", authentication);
    }

    @Test(expected = ExpiredJwtException.class)
    public void testExpiredToken() {
        String token = loadFileString("test/mockClientToken");
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        Date date = Date.from(LocalDate.of(2023, 07, 06).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        TokenProvider tokenProvider = new TokenProvider(() -> loadFile("test/public.cer"), new FixedClock(date));
        tokenProvider.getAuthentication(mockHttpServletRequest, token);
    }

    @Test(expected = SignatureException.class)
    public void testInvalidToken() {
        String token = loadFileString("test/mockInvalidClientToken");
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        Date date = Date.from(LocalDate.of(2023, 07, 06).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        TokenProvider tokenProvider = new TokenProvider(() -> loadFile("test/public.cer"), new FixedClock(date));
        tokenProvider.getAuthentication(mockHttpServletRequest, token);
    }

    @SneakyThrows
    public static byte[] loadFile(String path) {
        return loadFileString(path).getBytes();
    }

    @SneakyThrows
    public static String loadFileString(String path) {
        try (InputStream cfgInputStream = new ClassPathResource(path).getInputStream()) {
            return new String(cfgInputStream.readAllBytes(), UTF_8);
        }
    }


}
