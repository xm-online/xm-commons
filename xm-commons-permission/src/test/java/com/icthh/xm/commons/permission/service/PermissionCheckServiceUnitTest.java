package com.icthh.xm.commons.permission.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.permission.service.rolestrategy.RoleStrategy;
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

    private PermissionCheckService permissionCheckService;

    @Before
    public void before() {
        permissionCheckService = spy(
            new PermissionCheckService(multiRoleStrategy, singleRoleStrategy)
        );
    }

    @Test
    public void shouldCallMultiRoleHasPermission() {
        when(permissionCheckService.isMultiRoleEnabled(any())).thenReturn(true);
        when(multiRoleStrategy.hasPermission(any(), any())).thenReturn(true);

        boolean multipleResponse = permissionCheckService.hasPermission(any(), any());

        assertTrue(multipleResponse);
        verify(multiRoleStrategy, times(1)).hasPermission(any(), any());
    }

    @Test
    public void shouldCallMultiRoleHasPermissionResource() {
        when(permissionCheckService.isMultiRoleEnabled(any())).thenReturn(true);
        when(multiRoleStrategy.hasPermission(any(), any(), any())).thenReturn(true);
        boolean multipleResponse = permissionCheckService.hasPermission(any(), any(), any());

        assertTrue(multipleResponse);
        verify(multiRoleStrategy, times(1)).hasPermission(any(), any(), any());
    }

    @Test
    public void shouldCallMultiRoleHasPermissionResourceType() {
        when(permissionCheckService.isMultiRoleEnabled(any())).thenReturn(true);
        when(multiRoleStrategy.hasPermission(any(), any(), any(), any())).thenReturn(true);
        boolean multipleResponse = permissionCheckService.hasPermission(any(), any(), any(), any());

        assertTrue(multipleResponse);
        verify(multiRoleStrategy, times(1)).hasPermission(any(), any(), any(), any());
    }

    @Test
    public void shouldMultiRoleCreateCondition() {
        when(permissionCheckService.isMultiRoleEnabled(any())).thenReturn(true);

        when(multiRoleStrategy.createCondition(any(), any(), any())).thenReturn("Test");

        String condition = permissionCheckService.createCondition(any(), any(), any());

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
        when(singleRoleStrategy.createCondition(any(), any(), any())).thenReturn("Test");
        String condition = permissionCheckService.createCondition(any(), any(), any());

        assertNotNull(condition);
        assertFalse(condition.isEmpty());
        verify(singleRoleStrategy, times(1)).createCondition(any(), any(), any());
    }
}
