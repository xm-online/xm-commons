package com.icthh.xm.commons.permission.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class PermissionLepServiceUnitTest {

    private final PermissionLepService self = mock(PermissionLepService.class);
    private final PermissionLepService permissionLepService = new PermissionLepService();

    @Before
    public void before() {
        permissionLepService.setSelf(self);
    }

    @Test
    public void shouldDelegateCheckToRoleLep() {
        when(self.checkByRole("PRIVILEGE", "ROLE_USER", Map.of())).thenReturn(true);

        assertTrue(permissionLepService.check("PRIVILEGE", "ROLE_USER", Map.of()));
    }

    @Test
    public void shouldDelegateFilterToRoleLep() {
        when(self.filterByRole("PRIVILEGE", "ROLE_USER", Map.of())).thenReturn("a = 1");

        assertEquals("a = 1", permissionLepService.filter("PRIVILEGE", "ROLE_USER", Map.of()));
    }
}
