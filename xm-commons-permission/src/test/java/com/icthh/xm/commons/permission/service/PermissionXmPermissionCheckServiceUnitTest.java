package com.icthh.xm.commons.permission.service;

import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PermissionXmPermissionCheckServiceUnitTest {

    private static final String PRIVILEGE = "API_DOCS.GET";

    private PermissionCheckService permissionCheckService;
    private XmAuthenticationContext authContext;
    private TenantContextHolder tenantContextHolder;
    private PermissionXmPermissionCheckService service;

    @Before
    public void setUp() {
        permissionCheckService = mock(PermissionCheckService.class);
        authContext = mock(XmAuthenticationContext.class);
        XmAuthenticationContextHolder authContextHolder = mock(XmAuthenticationContextHolder.class);
        when(authContextHolder.getContext()).thenReturn(authContext);
        tenantContextHolder = new DefaultTenantContextHolder();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        SecurityContextHolder.clearContext();
        service = new PermissionXmPermissionCheckService(permissionCheckService, authContextHolder,
            tenantContextHolder);
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void deniesWithoutAuthentication() {
        assertThat(service.hasPermission(PRIVILEGE)).isFalse();
        verifyZeroInteractions(permissionCheckService);
    }

    @Test
    public void allowsSuperAdminWithoutPrivilegeCheck() {
        authenticate("SUPER-ADMIN");

        assertThat(service.hasPermission(PRIVILEGE)).isTrue();
        verifyZeroInteractions(permissionCheckService);
    }

    @Test
    public void checksPrivilegeInCurrentTenantWithContextAsResource() {
        Authentication authentication = authenticate("ROLE_USER");
        TenantContextUtils.setTenant(tenantContextHolder, "CURRENT");
        Map<String, Object> context = Map.of("id", 1);
        AtomicReference<Optional<String>> checkedInTenant = new AtomicReference<>();
        when(permissionCheckService.hasPermission(eq(authentication), eq(context), eq(PRIVILEGE)))
            .thenAnswer(invocation -> checkedInTenant.compareAndSet(null, currentTenant()));

        assertThat(service.hasPermission(PRIVILEGE, context)).isTrue();
        assertThat(checkedInTenant.get()).contains("CURRENT");
        assertThat(currentTenant()).contains("CURRENT");
    }

    @Test
    public void checksPrivilegeInTokenTenantWithoutTenantContext() {
        Authentication authentication = authenticate("ROLE_USER");
        when(authContext.isFullyAuthenticated()).thenReturn(true);
        when(authContext.getTenantName()).thenReturn(Optional.of("TOKEN"));
        AtomicReference<Optional<String>> checkedInTenant = new AtomicReference<>();
        when(permissionCheckService.hasPermission(eq(authentication), eq(Map.of()), eq(PRIVILEGE)))
            .thenAnswer(invocation -> checkedInTenant.compareAndSet(null, currentTenant()));

        assertThat(service.hasPermission(PRIVILEGE)).isTrue();
        assertThat(checkedInTenant.get()).contains("TOKEN");
        assertThat(currentTenant()).isEmpty();
    }

    @Test
    public void deniesAnonymousWithoutTenantContext() {
        authenticate("ROLE_ANONYMOUS");
        when(authContext.isFullyAuthenticated()).thenReturn(false);

        assertThat(service.hasPermission(PRIVILEGE)).isFalse();
        verifyZeroInteractions(permissionCheckService);
    }

    @Test
    public void deniesWhenRoleIsMissing() {
        authenticate("ROLE_UNKNOWN");
        TenantContextUtils.setTenant(tenantContextHolder, "CURRENT");
        when(permissionCheckService.hasPermission(any(), any(), eq(PRIVILEGE)))
            .thenThrow(new AccessDeniedException("Access is denied"));

        assertThat(service.hasPermission(PRIVILEGE)).isFalse();
    }

    @Test
    public void assertPermissionThrowsWhenDenied() {
        authenticate("ROLE_USER");
        TenantContextUtils.setTenant(tenantContextHolder, "CURRENT");
        when(permissionCheckService.hasPermission(any(), any(), eq(PRIVILEGE))).thenReturn(false);

        assertThatThrownBy(() -> service.assertPermission(PRIVILEGE)).isInstanceOf(AccessDeniedException.class);
    }

    private Authentication authenticate(String role) {
        Authentication authentication = new TestingAuthenticationToken("user", null, role);
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
        return authentication;
    }

    private Optional<String> currentTenant() {
        return TenantContextUtils.getTenantKey(tenantContextHolder).map(TenantKey::getValue);
    }
}
