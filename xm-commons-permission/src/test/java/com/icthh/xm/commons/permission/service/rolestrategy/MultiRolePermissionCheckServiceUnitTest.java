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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        Permission permission = mock(Permission.class);

        when(tenantContext.getTenantKey()).thenReturn(Optional.of(tenantKey));
        when(tenantKey.getValue()).thenReturn("testKey");
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(permissionService.getPermissions(any())).thenReturn(singletonMap("existingKey:privKey", permission));
        when(permission.getPrivilegeKey()).thenReturn("privKey");

        List<Permission> permissions = new ArrayList<>(multiRolePermissionCheckService
            .getPermissions(asList("existingKey", "notExistingKey"), "privKey"));

        assertFalse(permissions.isEmpty());
        assertEquals(1, permissions.size());
        assertNotNull(permissions.get(0));
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

    @Test
    public void shouldCreateMultipleCondition() {
        doReturn(true).when(multiRolePermissionCheckService).hasPermission(any(), any());
        doReturn(asList("ROLE_B2B_EXPERT", "ROLE_B2C_EXPERT")).when(multiRolePermissionCheckService).getRoleKeys(any());

        Permission permission = mock(Permission.class);
        Permission permission2 = mock(Permission.class);
        Expression expression = mock(Expression.class);
        Expression expression2 = mock(Expression.class);

        doReturn("subject.userKey == #subject.userKey").when(expression).getExpressionString();
        doReturn("subject.userKey == #subject.userKey").when(expression2).getExpressionString();
        doReturn(expression).when(permission).getResourceCondition();
        doReturn(expression2).when(permission2).getResourceCondition();
        doReturn("ROLE_B2B_EXPERT").when(permission).getRoleKey();
        doReturn("ROLE_B2C_EXPERT").when(permission2).getRoleKey();
        doReturn(asList(permission, permission2)).when(multiRolePermissionCheckService).getPermissions(any(), any());

        Map<String, Subject> stringSubjectHashMap = new HashMap<>();
        stringSubjectHashMap.put("ROLE_B2B_EXPERT", new Subject("", "user key", "ROLE_B2B_EXPERT"));
        stringSubjectHashMap.put("ROLE_B2C_EXPERT", new Subject("", "user key 2", "ROLE_B2C_EXPERT"));

        doReturn(stringSubjectHashMap).when(multiRolePermissionCheckService).getSubjects(any());

        String condition = multiRolePermissionCheckService.createCondition(
            new TestingAuthenticationToken(new Object(), new Object()), new Object(), spelToJpqlTranslator
        );

        assertFalse(condition.isEmpty());
        assertEquals("(subject.userKey  =  'user key') OR (subject.userKey  =  'user key 2')", condition);
    }
}
