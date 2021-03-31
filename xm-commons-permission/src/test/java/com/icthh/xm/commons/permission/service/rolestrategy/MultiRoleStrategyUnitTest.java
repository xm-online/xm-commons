package com.icthh.xm.commons.permission.service.rolestrategy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.service.PermissionService;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.PlainTenant;
import com.icthh.xm.commons.tenant.Tenant;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@RunWith(MockitoJUnitRunner.class)
public class MultiRoleStrategyUnitTest {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    @Mock
    Authentication authentication;

    @Mock
    XmAuthenticationContext xmAuthenticationContext;

    @Mock
    XmAuthenticationContextHolder xmAuthenticationContextHolder;

    @Mock
    TenantContextHolder tenantContextHolder;

    @Mock
    PermissionService permissionService;

    @InjectMocks
    MultiRoleStrategy multiRoleStrategy;

    final String privilege = "DO_ALL_PRIVILEGE";

    TenantContext CONTEXT = new TenantContext() {
        @Override
        public boolean isInitialized() {
            return true;
        }

        @Override
        public Optional<Tenant> getTenant() {
            return Optional.of(new PlainTenant(new TenantKey("XM")));
        }
    };

    @Test
    public void envConditionMustBePassed() {

        prepareMock("#env[ipAddress]=='127.0.0.1'", spelExpression -> new Permission() {{
            setEnvCondition(spelExpression);
        }});
        boolean result = multiRoleStrategy.checkPermission(authentication, null, privilege, false, false);
        assertTrue(result);
    }

    @Test
    public void envConditionMustBeDenied() {

        prepareMock("#env[ipAddress]=='127.0.0.2'", spelExpression -> new Permission() {{
            setEnvCondition(spelExpression);
        }});
        boolean result = multiRoleStrategy.checkPermission(authentication, null, privilege, false, false);
        assertFalse(result);
    }


    @Test
    public void resourceConditionMustBePassed() {

        prepareMock("#firstName=='Homer'", spelExpression -> new Permission() {{
            setResourceCondition(spelExpression);
        }});
        boolean result = multiRoleStrategy.checkPermission(authentication, Map.of("firstName", "Homer"), privilege, true, false);
        assertTrue(result);
    }

    @Test
    public void resourceConditionMustBeDenied() {

        prepareMock("#firstName=='Bart'", spelExpression -> new Permission() {{
            setResourceCondition(spelExpression);
        }});
        boolean result = multiRoleStrategy.checkPermission(authentication, Map.of("firstName", "Homer"), privilege, true, false);
        assertFalse(result);
    }

    private void prepareMock(String spel, Function<SpelExpression, Permission > permissionBuilder){
        when(authentication.getAuthorities()).thenAnswer((Answer<Object>) invocation -> List.of(new SimpleGrantedAuthority(ROLE_ADMIN)));
        when(xmAuthenticationContextHolder.getContext()).thenReturn(xmAuthenticationContext);
        when(xmAuthenticationContext.getRemoteAddress()).thenReturn(Optional.of("127.0.0.1"));
        when(tenantContextHolder.getContext()).thenReturn(CONTEXT);
        SpelExpressionParser spelExpressionParser = new SpelExpressionParser();
        SpelExpression spelExpression = spelExpressionParser.parseRaw(spel);
        Permission permission = permissionBuilder.apply(spelExpression);
        when(permissionService.getPermissions(eq("XM"))).thenReturn(Map.of(ROLE_ADMIN + ":" + privilege, permission));
    }
}
