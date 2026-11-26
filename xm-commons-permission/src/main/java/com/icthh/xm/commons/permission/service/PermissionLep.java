package com.icthh.xm.commons.permission.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
        if (StringUtils.isBlank(lepSegment)) {
            return false;
        }
        return permissionLepService.check(lepSegment, privilege, roleKey, params, contextParams);
    }

    public String filter(String lepSegment) {
        return filter(lepSegment, Map.of());
    }

    public String filter(String lepSegment, Map<String, Object> params) {
        if (StringUtils.isBlank(lepSegment)) {
            throw new AccessDeniedException("Access is denied");
        }
        String filter = permissionLepService.filter(lepSegment, privilege, roleKey, params, contextParams);
        if (StringUtils.isBlank(filter)) {
            throw new AccessDeniedException("Access is denied");
        }
        return filter;
    }
}
