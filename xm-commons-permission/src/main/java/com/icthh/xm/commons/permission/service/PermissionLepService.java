package com.icthh.xm.commons.permission.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.keyresolver.PrivilegeKeyResolver;
import com.icthh.xm.commons.permission.keyresolver.PrivilegeRoleKeyResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

@Slf4j
@LepService(group = "service.permission")
public class PermissionLepService {

    private PermissionLepService self;

    @LogicExtensionPoint(value = "Check", resolver = PrivilegeKeyResolver.class)
    public boolean check(String privilege, String roleKey, Map<String, Object> resources) {
        return self.checkByRole(privilege, roleKey, resources);
    }

    @LogicExtensionPoint(value = "Check", resolver = PrivilegeRoleKeyResolver.class)
    public boolean checkByRole(String privilege, String roleKey, Map<String, Object> resources) {
        log.warn("Permission check LEP is not defined, access denied: privilege={}, role={}", privilege, roleKey);
        return false;
    }

    @LogicExtensionPoint(value = "Filter", resolver = PrivilegeKeyResolver.class)
    public String filter(String privilege, String roleKey, Map<String, Object> resources) {
        return self.filterByRole(privilege, roleKey, resources);
    }

    @LogicExtensionPoint(value = "Filter", resolver = PrivilegeRoleKeyResolver.class)
    public String filterByRole(String privilege, String roleKey, Map<String, Object> resources) {
        log.warn("Permission filter LEP is not defined, access denied: privilege={}, role={}", privilege, roleKey);
        throw new AccessDeniedException("Access is denied");
    }

    @Autowired
    public void setSelf(@Lazy PermissionLepService self) {
        this.self = self;
    }
}
