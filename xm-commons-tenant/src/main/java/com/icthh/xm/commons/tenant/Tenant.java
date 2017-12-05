package com.icthh.xm.commons.tenant;

/**
 * The {@link Tenant} interface.
 */
public interface Tenant {

    TenantKey getTenantKey();

    boolean isSuper();

}
