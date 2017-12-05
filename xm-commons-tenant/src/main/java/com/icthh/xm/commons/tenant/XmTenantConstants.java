package com.icthh.xm.commons.tenant;

/**
 * The {@link XmTenantConstants} class.
 */
public final class XmTenantConstants {

    /**
     * HTTP request header for tenant name.
     */
    public static final String HTTP_HEADER_TENANT_NAME = "x-tenant";

    /**
     * XM authentication context 'details' value key for tenant name.
     */
    public static final String AUTH_CONTEXT_TENANT_NAME = "tenant";

    /**
     * Utilities class constructor.
     */
    private XmTenantConstants() {
        throw new IllegalAccessError("utilities constructor access not allowed");
    }

}
