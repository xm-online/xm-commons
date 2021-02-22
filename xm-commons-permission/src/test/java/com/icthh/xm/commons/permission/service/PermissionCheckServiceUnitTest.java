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

import com.icthh.xm.commons.permission.utils.SecurityUtils;
import java.util.Collection;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PermissionCheckServiceUnitTest {

    @Mock
    private MultiRolePermissionCheckService multiRolePermissionCheckService;

    @Mock
    private SingleRolePermissionCheckService singleRolePermissionCheckService;

    @Mock
    private SecurityUtils securityUtils;

    private PermissionCheckService permissionCheckService;

    @Before
    public void before() {
        permissionCheckService = spy(
            new PermissionCheckService(securityUtils, multiRolePermissionCheckService, singleRolePermissionCheckService)
        );
    }

    @Test
    public void shouldCallMultiRoleHasPermission() {
        when(securityUtils.getAdditionalDetailsValueBoolean(any(),any())).thenReturn(Optional.of(true));
        when(multiRolePermissionCheckService.hasPermission(any(), any())).thenReturn(true);

        boolean multipleResponse = permissionCheckService.hasPermission(any(), any());

        assertTrue(multipleResponse);
        verify(multiRolePermissionCheckService, times(1)).hasPermission(any(), any());
    }

    @Test
    public void shouldCallMultiRoleHasPermissionResource() {
        when(securityUtils.getAdditionalDetailsValueBoolean(any(),any())).thenReturn(Optional.of(true));
        when(multiRolePermissionCheckService.hasPermission(any(), any(), any())).thenReturn(true);
        boolean multipleResponse = permissionCheckService.hasPermission(any(), any(), any());

        assertTrue(multipleResponse);
        verify(multiRolePermissionCheckService, times(1)).hasPermission(any(), any(), any());
    }

    @Test
    public void shouldCallMultiRoleHasPermissionResourceType() {
        when(securityUtils.getAdditionalDetailsValueBoolean(any(),any())).thenReturn(Optional.of(true));
        when(multiRolePermissionCheckService.hasPermission(any(), any(), any(), any())).thenReturn(true);
        boolean multipleResponse = permissionCheckService.hasPermission(any(), any(), any(), any());

        assertTrue(multipleResponse);
        verify(multiRolePermissionCheckService, times(1)).hasPermission(any(), any(), any(), any());
    }

    @Test
    public void shouldMultiRoleCreateCondition() {
        when(securityUtils.getAdditionalDetailsValueBoolean(any(),any())).thenReturn(Optional.of(true));
        when(multiRolePermissionCheckService.createCondition(any(), any(), any())).thenReturn(singletonList("Test"));

        Collection<String> condition = permissionCheckService.createCondition(any(), any(), any());

        assertNotNull(condition);
        assertFalse(condition.isEmpty());
        verify(multiRolePermissionCheckService, times(1)).createCondition(any(), any(), any());
    }

    @Test
    public void shouldCallSingleRoleHasPermission() {
        when(singleRolePermissionCheckService.hasPermission(any(), any())).thenReturn(true);
        boolean multipleResponse = permissionCheckService.hasPermission(any(), any());

        assertTrue(multipleResponse);
        verify(singleRolePermissionCheckService, times(1)).hasPermission(any(), any());
    }

    @Test
    public void shouldCallSingleRoleHasPermissionResource() {
        when(singleRolePermissionCheckService.hasPermission(any(), any(), any())).thenReturn(true);
        boolean multipleResponse = permissionCheckService.hasPermission(any(), any(), any());

        assertTrue(multipleResponse);
        verify(singleRolePermissionCheckService, times(1)).hasPermission(any(), any(), any());
    }

    @Test
    public void shouldCallSingleRoleHasPermissionResourceType() {
        when(singleRolePermissionCheckService.hasPermission(any(), any(), any(), any())).thenReturn(true);
        boolean multipleResponse = permissionCheckService.hasPermission(any(), any(), any(), any());

        assertTrue(multipleResponse);
        verify(singleRolePermissionCheckService, times(1)).hasPermission(any(), any(), any(), any());
    }

    @Test
    public void shouldSingleCreateCondition() {
        when(singleRolePermissionCheckService.createCondition(any(), any(), any())).thenReturn("Test");
        Collection<String> condition = permissionCheckService.createCondition(any(), any(), any());

        assertNotNull(condition);
        assertFalse(condition.isEmpty());
        verify(singleRolePermissionCheckService, times(1)).createCondition(any(), any(), any());
    }
}
