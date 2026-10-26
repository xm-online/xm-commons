package com.icthh.xm.commons.security.spring.config;

import com.icthh.xm.commons.security.XmPermissionCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class XmAccessPrivileges {

    private final XmPermissionCheckService permissionCheckService;

    private final Set<String> accessPrivileges = ConcurrentHashMap.newKeySet();

    public String access(String privilege) {
        accessPrivileges.add(privilege);
        return "@xmAccessPrivileges.hasPermission('" + privilege + "')";
    }

    public boolean hasPermission(String privilege) {
        return permissionCheckService.hasPermission(privilege);
    }

    public Set<String> getAccessPrivileges() {
        return Set.copyOf(accessPrivileges);
    }
}
