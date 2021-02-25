package com.icthh.xm.commons.permission.service;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.permission.service.rolestrategy.RoleStrategy;
import com.icthh.xm.commons.permission.utils.SecurityUtils;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PermissionCheckServiceUnitTest {

    @Mock
    private RoleStrategy multiRoleStrategy;

    @Mock
    private RoleStrategy singleRoleStrategy;

    @Mock
    private SecurityUtils securityUtils;

    private PermissionCheckService permissionCheckService;

    @Before
    public void before() {
        permissionCheckService = spy(
            new PermissionCheckService(securityUtils, multiRoleStrategy, singleRoleStrategy)
        );
    }

    @Test
    public void shouldCallMultiRoleHasPermission() {
        when(securityUtils.getAdditionalDetailsValueBoolean(any(),any())).thenReturn(true);
        when(multiRoleStrategy.hasPermission(any(), any())).thenReturn(true);

        boolean multipleResponse = permissionCheckService.hasPermission(any(), any());

        assertTrue(multipleResponse);
        verify(multiRoleStrategy, times(1)).hasPermission(any(), any());
    }

    @Test
    public void shouldCallMultiRoleHasPermissionResource() {
        when(securityUtils.getAdditionalDetailsValueBoolean(any(),any())).thenReturn(true);
        when(multiRoleStrategy.hasPermission(any(), any(), any())).thenReturn(true);
        boolean multipleResponse = permissionCheckService.hasPermission(any(), any(), any());

        assertTrue(multipleResponse);
        verify(multiRoleStrategy, times(1)).hasPermission(any(), any(), any());
    }

    @Test
    public void shouldCallMultiRoleHasPermissionResourceType() {
        when(securityUtils.getAdditionalDetailsValueBoolean(any(),any())).thenReturn(true);
        when(multiRoleStrategy.hasPermission(any(), any(), any(), any())).thenReturn(true);
        boolean multipleResponse = permissionCheckService.hasPermission(any(), any(), any(), any());

        assertTrue(multipleResponse);
        verify(multiRoleStrategy, times(1)).hasPermission(any(), any(), any(), any());
    }

    @Test
    public void shouldMultiRoleCreateCondition() {
        when(securityUtils.getAdditionalDetailsValueBoolean(any(),any())).thenReturn(true);
        when(multiRoleStrategy.createCondition(any(), any(), any())).thenReturn(singletonList("Test"));

        Collection<String> condition = permissionCheckService.createCondition(any(), any(), any());

        assertNotNull(condition);
        assertFalse(condition.isEmpty());
        verify(multiRoleStrategy, times(1)).createCondition(any(), any(), any());
    }

    @Test
    public void shouldCallSingleRoleHasPermission() {
        when(singleRoleStrategy.hasPermission(any(), any())).thenReturn(true);
        boolean multipleResponse = permissionCheckService.hasPermission(any(), any());

        assertTrue(multipleResponse);
        verify(singleRoleStrategy, times(1)).hasPermission(any(), any());
    }

    @Test
    public void shouldCallSingleRoleHasPermissionResource() {
        when(singleRoleStrategy.hasPermission(any(), any(), any())).thenReturn(true);
        boolean multipleResponse = permissionCheckService.hasPermission(any(), any(), any());

        assertTrue(multipleResponse);
        verify(singleRoleStrategy, times(1)).hasPermission(any(), any(), any());
    }

    @Test
    public void shouldCallSingleRoleHasPermissionResourceType() {
        when(singleRoleStrategy.hasPermission(any(), any(), any(), any())).thenReturn(true);
        boolean multipleResponse = permissionCheckService.hasPermission(any(), any(), any(), any());

        assertTrue(multipleResponse);
        verify(singleRoleStrategy, times(1)).hasPermission(any(), any(), any(), any());
    }

    @Test
    public void shouldSingleCreateCondition() {
        when(singleRoleStrategy.createCondition(any(), any(), any())).thenReturn(singletonList("Test"));
        Collection<String> condition = permissionCheckService.createCondition(any(), any(), any());

        assertNotNull(condition);
        assertFalse(condition.isEmpty());
        verify(singleRoleStrategy, times(1)).createCondition(any(), any(), any());
    }
}
