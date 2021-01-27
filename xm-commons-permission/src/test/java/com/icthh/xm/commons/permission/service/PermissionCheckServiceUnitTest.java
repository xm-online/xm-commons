package com.icthh.xm.commons.permission.service;

import com.icthh.xm.commons.permission.access.ResourceFactory;
import com.icthh.xm.commons.permission.access.subject.Subject;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.service.translator.SpelToJpqlTranslator;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.expression.Expression;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.icthh.xm.commons.permission.constants.RoleConstant.SUPER_ADMIN;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PermissionCheckServiceUnitTest {

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

    private PermissionCheckService permissionCheckService;

    @Before
    public void before() {
        permissionCheckService = spy(new PermissionCheckService(
            xmAuthenticationContextHolder,
            tenantContextHolder,
            permissionService,
            resourceFactory,
            roleService
        ));
    }


    @Test
    public void shouldCreateEmptyConditionForSuperAdmin() {
        doReturn(true).when(permissionCheckService).hasPermission(any(), any());
        doReturn(singletonList(SUPER_ADMIN)).when(permissionCheckService).getRoleKeys(any());

        Collection<String> condition = permissionCheckService.createCondition(any(), any(), any());

        assertNotNull(condition);
        assertTrue(condition.isEmpty());
    }

    @Test
    public void shouldCreateCondition() {
        doReturn(true).when(permissionCheckService).hasPermission(any(), any());
        doReturn(singletonList("ROLE_B2B_EXPERT")).when(permissionCheckService).getRoleKeys(any());

        Permission permission = mock(Permission.class);
        Expression expression = mock(Expression.class);

        doReturn("subject.userKey == #subject.userKey").when(expression).getExpressionString();
        doReturn(expression).when(permission).getResourceCondition();
        doReturn("ROLE_B2B_EXPERT").when(permission).getRoleKey();
        doReturn(singletonList(permission)).when(permissionCheckService).getPermissions(any(), any());
        doReturn(singletonMap("ROLE_B2B_EXPERT", new Subject("", "user key", "ROLE_B2B_EXPERT"))).when(permissionCheckService).getSubjects(any());

        List<String> condition = new ArrayList<>(permissionCheckService.createCondition(
            new TestingAuthenticationToken(new Object(), new Object()), new Object(), spelToJpqlTranslator
        ));

        assertFalse(condition.isEmpty());
        assertEquals(condition.get(0), "subject.userKey  =  'user key'");
    }
}
