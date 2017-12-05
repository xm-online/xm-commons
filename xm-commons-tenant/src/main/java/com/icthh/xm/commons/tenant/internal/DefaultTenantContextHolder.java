package com.icthh.xm.commons.tenant.internal;

import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;

/**
 * The {@link DefaultTenantContextHolder} class.
 */
public class DefaultTenantContextHolder implements TenantContextHolder {

    /**
     * {@inheritDoc}
     */
    @Override
    public TenantContext getContext() {
        return new ThreadLocalTenantContext(TenantContextDataHolder.getThreadLocalInstance());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrivilegedTenantContext getPrivilegedContext() {
        return new ThreadLocalPrivilegedTenantContext(TenantContextDataHolder.getThreadLocalInstance());
    }

}
