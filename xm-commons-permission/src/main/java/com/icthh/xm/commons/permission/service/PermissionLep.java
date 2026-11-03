package com.icthh.xm.commons.permission.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

@RequiredArgsConstructor
public class PermissionLep {

    private final PermissionLepService permissionLepService;
    private final String privilege;
    private final String roleKey;
    private final Map<String, Object> contextParams;

    public boolean check(String lepSegment) {
        return check(lepSegment, Map.of());
    }

    public boolean check(String lepSegment, Map<String, Object> params) {
        return permissionLepService.check(lepSegment, privilege, roleKey, params, contextParams);
    }

    public String filter(String lepSegment) {
        return filter(lepSegment, Map.of());
    }

    public String filter(String lepSegment, Map<String, Object> params) {
        String filter = permissionLepService.filter(lepSegment, privilege, roleKey, params, contextParams);
        if (filter == null) {
            throw new AccessDeniedException("Access is denied");
        }
        return filter;
    }
}
