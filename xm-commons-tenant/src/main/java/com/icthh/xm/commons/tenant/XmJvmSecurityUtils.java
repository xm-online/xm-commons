package com.icthh.xm.commons.tenant;

import java.lang.management.ManagementPermission;

/**
 * The {@link XmJvmSecurityUtils} class.
 */
public final class XmJvmSecurityUtils {

    /**
     * JVM 'control' permission name.
     */
    public static final String PERMISSION_NAME_CONTROL = "control";

    /**
     * If JVM security manager exists then checks JVM security 'control' permission.
     */
    public static void checkSecurity() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new ManagementPermission(PERMISSION_NAME_CONTROL));
        }
    }

    /**
     * Utils constructor.
     */
    private XmJvmSecurityUtils() {
        throw new IllegalAccessError("utils class constructor");
    }

}
