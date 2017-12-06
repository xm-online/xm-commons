package com.icthh.xm.commons.permission.access;

import com.icthh.xm.commons.permission.service.PermissionCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@RequiredArgsConstructor
public class XmPermissionEvaluator implements PermissionEvaluator {

    private final PermissionCheckService service;

    @Override
    public boolean hasPermission(Authentication authentication, Object resource, Object privilege) {
        return service.hasPermission(authentication, resource, privilege);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable resource, String resourceType,
                                 Object privilege) {
        return service.hasPermission(authentication, resource, resourceType, privilege);
    }
}
