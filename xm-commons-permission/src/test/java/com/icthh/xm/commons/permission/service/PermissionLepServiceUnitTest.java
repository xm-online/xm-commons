package com.icthh.xm.commons.permission.service;

import static org.junit.Assert.assertFalse;

import java.util.Map;
import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;

public class PermissionLepServiceUnitTest {

    private final PermissionLepService permissionLepService = new PermissionLepService();

    @Test
    public void shouldDenyCheckWithoutLep() {
        assertFalse(permissionLepService.check("company", "PRIVILEGE", "ROLE_USER", Map.of(), Map.of()));
    }

    @Test(expected = AccessDeniedException.class)
    public void shouldDenyFilterWithoutLep() {
        permissionLepService.filter("company", "PRIVILEGE", "ROLE_USER", Map.of(), Map.of());
    }
}
