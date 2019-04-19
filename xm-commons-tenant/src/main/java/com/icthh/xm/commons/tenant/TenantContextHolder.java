package com.icthh.xm.commons.tenant;

/**
 * The {@link TenantContextHolder} interface.
 */
public interface TenantContextHolder {

    /**
     * Obtain the current {@code TenantContext}.
     *
     * @return the tenant context (never {@code null})
     */
    TenantContext getContext();

    /**
     * Obtain the current {@code PrivilegedTenantContext}.
     *
     * @return the privileged tenant context (never {@code null})
     */
    PrivilegedTenantContext getPrivilegedContext();

    default String getTenantKey() {
        return TenantContextUtils.getRequiredTenantKeyValue(this);
    }

}
