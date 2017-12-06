package com.icthh.xm.commons.tenant;

import java.util.Optional;

/**
 * The {@link TenantContext} interface.
 */
public interface TenantContext {

    boolean isInitialized();

    /**
     * Gets context tenant object.
     *
     * @return {@link Tenant} object
     */
    Optional<Tenant> getTenant();

    /**
     * Gets tenant key object.
     *
     * @return the tenant key object
     */
    default Optional<TenantKey> getTenantKey() {
        return getTenant().map(Tenant::getTenantKey);
    }

}
