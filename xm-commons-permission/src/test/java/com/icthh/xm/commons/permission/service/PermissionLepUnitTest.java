package com.icthh.xm.commons.permission.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;

public class PermissionLepUnitTest {

    private static final String PRIVILEGE = "PRIVILEGE";
    private static final String ROLE = "ROLE_USER";

    private final PermissionLepService permissionLepService = mock(PermissionLepService.class);
    private final PermissionLep permissionLep =
        new PermissionLep(permissionLepService, PRIVILEGE, ROLE, Map.of());

    @Test
    public void shouldDenyCheckWithNullLepSegment() {
        assertFalse(permissionLep.check(null));
        verifyZeroInteractions(permissionLepService);
    }

    @Test
    public void shouldDenyCheckWithBlankLepSegment() {
        assertFalse(permissionLep.check("   "));
        verifyZeroInteractions(permissionLepService);
    }

    @Test(expected = AccessDeniedException.class)
    public void shouldDenyFilterWithNullLepSegment() {
        permissionLep.filter(null);
    }

    @Test(expected = AccessDeniedException.class)
    public void shouldDenyFilterWithBlankLepSegment() {
        permissionLep.filter("   ");
    }

    @Test
    public void shouldDelegateCheckWhenLepSegmentIsPresent() {
        when(permissionLepService.check("company", PRIVILEGE, ROLE, Map.of(), Map.of())).thenReturn(true);

        assertEquals(true, permissionLep.check("company"));
    }

    @Test(expected = AccessDeniedException.class)
    public void shouldDenyFilterWhenLepReturnsBlank() {
        when(permissionLepService.filter("company", PRIVILEGE, ROLE, Map.of(), Map.of())).thenReturn("  ");

        permissionLep.filter("company");
    }
}
