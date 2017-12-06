package com.icthh.xm.commons.tenant.internal;

import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.Tenant;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.commons.tenant.XmJvmSecurityUtils;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The {@link ThreadLocalPrivilegedTenantContext} class.
 */
final class ThreadLocalPrivilegedTenantContext implements PrivilegedTenantContext {

    private final TenantContextDataHolder tenantContextDataHolder;
    private final TenantContext tenantContextDelegate;

    /**
     * Creates a TenantContext using the given TenantContext holder as its backing instance.
     *
     * @param tenantContextDataHolder the TenantContext holder that backs this TenantContext object.
     * @see TenantContextDataHolder
     */
    ThreadLocalPrivilegedTenantContext(TenantContextDataHolder tenantContextDataHolder) {
        this.tenantContextDataHolder = tenantContextDataHolder;
        this.tenantContextDelegate = new ThreadLocalTenantContext(tenantContextDataHolder);
    }

    private TenantContextDataHolder getDataHolder() {
        return tenantContextDataHolder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTenant(Tenant tenant) {
        getDataHolder().setTenant(tenant);
    }

    /**
     * Destroys context instance for current thread.
     * Need to be called for each thread where used {@link ThreadLocalPrivilegedTenantContext}.
     */
    @Override
    public void destroyCurrentContext() {
        XmJvmSecurityUtils.checkSecurity();
        getDataHolder().destroyForCurrentThread();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V> V execute(Tenant tenant, Supplier<V> supplier) {
        return TenantContextDataHolder.execute(tenant, supplier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Tenant tenant, Runnable runnable) {
        TenantContextDataHolder.execute(tenant, runnable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V> void execute(Tenant tenant, Consumer<V> consumer, V value) {
        TenantContextDataHolder.execute(tenant, consumer, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInitialized() {
        return tenantContextDelegate.isInitialized();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Tenant> getTenant() {
        return tenantContextDelegate.getTenant();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<TenantKey> getTenantKey() {
        return tenantContextDelegate.getTenantKey();
    }

}
