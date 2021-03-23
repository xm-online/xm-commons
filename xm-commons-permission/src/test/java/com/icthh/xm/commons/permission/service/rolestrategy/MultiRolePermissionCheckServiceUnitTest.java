package com.icthh.xm.commons.permission.service.rolestrategy;

import static com.icthh.xm.commons.permission.constants.RoleConstant.SUPER_ADMIN;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.permission.access.ResourceFactory;
import com.icthh.xm.commons.permission.access.subject.Subject;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.service.PermissionService;
import com.icthh.xm.commons.permission.service.RoleService;
import com.icthh.xm.commons.permission.service.translator.SpelToJpqlTranslator;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import java.util.Collection;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.expression.Expression;
import org.springframework.security.authentication.TestingAuthenticationToken;

@RunWith(MockitoJUnitRunner.class)
public class MultiRolePermissionCheckServiceUnitTest {

    @Mock
    private XmAuthenticationContextHolder xmAuthenticationContextHolder;
    @Mock
    private TenantContextHolder tenantContextHolder;
    @Mock
    private PermissionService permissionService;
    @Mock
    private ResourceFactory resourceFactory;
    @Mock
    private RoleService roleService;

    private final SpelToJpqlTranslator spelToJpqlTranslator = new SpelToJpqlTranslator();

    private MultiRoleStrategy multiRolePermissionCheckService;

    @Before
    public void before() {
        multiRolePermissionCheckService = spy(new MultiRoleStrategy(
            xmAuthenticationContextHolder,
            tenantContextHolder,
            permissionService,
            resourceFactory,
            roleService
        ));
    }

    @Test
    public void shouldNotReturnNullInPermissionList() {
        TenantContext tenantContext = mock(TenantContext.class);
        TenantKey tenantKey = mock(TenantKey.class);

        when(tenantContext.getTenantKey()).thenReturn(Optional.of(tenantKey));
        when(tenantKey.getValue()).thenReturn("testKey");
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(permissionService.getPermissions(any())).thenReturn(singletonMap("existingKey:privKey", mock(Permission.class)));

        Collection<Permission> permissions = multiRolePermissionCheckService
            .getPermissions(asList("existingKey", "notExistingKey"), "privKey");

        assertFalse(permissions.isEmpty());
        assertEquals(1, permissions.size());
    }

    @Test
    public void shouldCreateEmptyConditionForSuperAdmin() {
        doReturn(true).when(multiRolePermissionCheckService).hasPermission(any(), any());
        doReturn(singletonList(SUPER_ADMIN)).when(multiRolePermissionCheckService).getRoleKeys(any());

        String condition = multiRolePermissionCheckService.createCondition(any(), any(), any());

        assertNotNull(condition);
        assertTrue(condition.isEmpty());
    }

    @Test
    public void shouldCreateCondition() {
        doReturn(true).when(multiRolePermissionCheckService).hasPermission(any(), any());
        doReturn(singletonList("ROLE_B2B_EXPERT")).when(multiRolePermissionCheckService).getRoleKeys(any());

        Permission permission = mock(Permission.class);
        Expression expression = mock(Expression.class);

        doReturn("subject.userKey == #subject.userKey").when(expression).getExpressionString();
        doReturn(expression).when(permission).getResourceCondition();
        doReturn("ROLE_B2B_EXPERT").when(permission).getRoleKey();
        doReturn(singletonList(permission)).when(multiRolePermissionCheckService).getPermissions(any(), any());
        doReturn(singletonMap("ROLE_B2B_EXPERT", new Subject("", "user key", "ROLE_B2B_EXPERT")))
            .when(multiRolePermissionCheckService)
            .getSubjects(any());

        String condition = multiRolePermissionCheckService.createCondition(
            new TestingAuthenticationToken(new Object(), new Object()), new Object(), spelToJpqlTranslator
        );

        assertFalse(condition.isEmpty());
        assertEquals("(subject.userKey  =  'user key')", condition);
    }
}
