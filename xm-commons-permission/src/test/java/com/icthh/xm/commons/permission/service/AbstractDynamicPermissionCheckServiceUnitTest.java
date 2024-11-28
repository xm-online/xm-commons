package com.icthh.xm.commons.permission.service;

import com.icthh.xm.commons.permission.domain.enums.IFeatureContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.security.internal.SpringSecurityXmAuthenticationContextHolder;
import com.icthh.xm.commons.security.internal.XmAuthentication;
import com.icthh.xm.commons.security.internal.XmAuthenticationDetails;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbstractDynamicPermissionCheckServiceUnitTest {

    private static final String PRIVILEGE_TEST = "APPLICATION.ACCOUNT";

    private DynamicPermissionCheckService dynamicPermissionCheckService;

    @Mock
    private PermissionCheckService permissionCheckService;

    private IFeatureContext mockFeatureContext = mock(IFeatureContext.class);

    private XmAuthentication mockAuthentication = mock(XmAuthentication.class);

    private XmAuthenticationContextHolder authenticationContextHolder = new SpringSecurityXmAuthenticationContextHolder();


    @Before
    public void setUp() {
        when(mockAuthentication.getAuthorities()).thenReturn(
            Collections.singleton((GrantedAuthority) () -> "roleKeyTest")
        );

        XmAuthenticationDetails details = mock(XmAuthenticationDetails.class);
        when(details.getDecodedDetails()).thenReturn(Map.of("user_key", "userKeyTest"));

        when(mockAuthentication.getDetails()).thenReturn(details);

        SecurityContextImpl securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(mockAuthentication);
        SecurityContextHolder.setContext(securityContext);

        dynamicPermissionCheckService = new AbstractDynamicPermissionCheckService(permissionCheckService, authenticationContextHolder) {};
    }

    @Test
    public void checkContextPermission() {
        when(mockFeatureContext.isEnabled(any())).thenReturn(true);
        when(permissionCheckService.hasPermission(mockAuthentication, PRIVILEGE_TEST + ".suffix")).thenReturn(true);

        boolean result = dynamicPermissionCheckService.checkContextPermission(mockFeatureContext, PRIVILEGE_TEST, "suffix");

        assertTrue(result);
        verify(permissionCheckService).hasPermission(mockAuthentication, PRIVILEGE_TEST + ".suffix");
    }

    @Test
    public void checkContextPermission_featureDisabled() {
        when(mockFeatureContext.isEnabled(any())).thenReturn(false);
        when(permissionCheckService.hasPermission(mockAuthentication, PRIVILEGE_TEST)).thenReturn(true);

        boolean result = dynamicPermissionCheckService.checkContextPermission(mockFeatureContext, PRIVILEGE_TEST, "suffix");

        assertTrue(result);
        verify(permissionCheckService).hasPermission(mockAuthentication, PRIVILEGE_TEST);
    }

    @Test
    public void checkContextPermission_accessDenied() {
        when(mockFeatureContext.isEnabled(any())).thenReturn(false);
        when(permissionCheckService.hasPermission(mockAuthentication, PRIVILEGE_TEST)).thenReturn(false);

        String errorMsg = "access denied: privilege=APPLICATION.ACCOUNT, roleKey=roleKeyTest, user=userKeyTest due to privilege is not permitted";

        assertThrows(errorMsg, AccessDeniedException.class,
            () -> dynamicPermissionCheckService.checkContextPermission(mockFeatureContext, PRIVILEGE_TEST, "suffix"));
    }

    @Test
    public void checkContextPermission_emptyBasePermission() {
        assertThrows(IllegalArgumentException.class,
            () -> dynamicPermissionCheckService.checkContextPermission(mockFeatureContext, StringUtils.EMPTY, "suffix"));
    }

    @Test
    public void checkContextPermission_emptySuffix() {
        assertThrows(IllegalArgumentException.class,
            () -> dynamicPermissionCheckService.checkContextPermission(mockFeatureContext, "base", StringUtils.EMPTY));
    }
}
