package com.icthh.xm.commons.security;

import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

public interface XmPermissionCheckService {

    boolean hasPermission(String privilege, Map<String, Object> context);

    default boolean hasPermission(String privilege) {
        return hasPermission(privilege, Map.of());
    }

    default void assertPermission(String privilege, Map<String, Object> context) {
        if (!hasPermission(privilege, context)) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    default void assertPermission(String privilege) {
        assertPermission(privilege, Map.of());
    }
}
