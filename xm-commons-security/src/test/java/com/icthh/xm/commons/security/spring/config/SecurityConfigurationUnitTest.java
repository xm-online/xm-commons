package com.icthh.xm.commons.security.spring.config;

import com.icthh.xm.commons.security.XmPermissionCheckService;
import com.icthh.xm.commons.security.jwt.TokenProvider;
import org.junit.Test;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecurityConfigurationUnitTest {

    private final XmPermissionCheckService permissionCheckService = mock(XmPermissionCheckService.class);

    private final SecurityConfiguration securityConfiguration =
        new SecurityConfiguration(mock(TokenProvider.class), "default-src 'self'", permissionCheckService);

    @Test
    public void testAccessIsDecidedByPrivilege() {
        AuthorizationManager<RequestAuthorizationContext> access = securityConfiguration.access("TEST.GET");

        when(permissionCheckService.hasPermission("TEST.GET")).thenReturn(true);
        assertTrue(access.authorize(() -> null, null).isGranted());

        when(permissionCheckService.hasPermission("TEST.GET")).thenReturn(false);
        assertFalse(access.authorize(() -> null, null).isGranted());
    }

    @Test
    public void testAccessPrivilegesAreCollected() {
        securityConfiguration.access("TEST.GET");
        securityConfiguration.access("TEST.UPDATE");

        assertEquals(Set.of("TEST.GET", "TEST.UPDATE"), securityConfiguration.getAccessPrivileges());
    }
}
