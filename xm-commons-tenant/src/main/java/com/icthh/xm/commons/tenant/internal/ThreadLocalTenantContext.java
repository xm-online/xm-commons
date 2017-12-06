package com.icthh.xm.commons.tenant.internal;

import com.icthh.xm.commons.tenant.Tenant;
import com.icthh.xm.commons.tenant.TenantContext;

import java.util.Optional;

/**
 * The {@link ThreadLocalTenantContext} class.
 */
final class ThreadLocalTenantContext implements TenantContext {

    /**
     * Internal holder instance.
     */
    private final TenantContextDataHolder tenantContextDataHolder;

    /**
     * Creates a TenantContext using the given TenantContext holder as its backing instance.
     *
     * @param tenantContextDataHolder the TenantContext holder that backs this TenantContext object.
     * @see TenantContextDataHolder
     */
    ThreadLocalTenantContext(TenantContextDataHolder tenantContextDataHolder) {
        this.tenantContextDataHolder = tenantContextDataHolder;
    }

    /**
     * Method to obtain the current TenantContext holder after an instance of a
     * TenantContext has been created.
     *
     * @return the current TenantContext data holder
     */
    private TenantContextDataHolder getTenantContextDataHolder() {
        return tenantContextDataHolder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInitialized() {
        return getTenantContextDataHolder().isInitialized();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Tenant> getTenant() {
        return getTenantContextDataHolder().getTenant();
    }

}
