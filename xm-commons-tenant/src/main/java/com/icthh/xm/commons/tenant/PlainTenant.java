package com.icthh.xm.commons.tenant;

import java.util.Objects;

/**
 * The {@link PlainTenant} class.
 */
public class PlainTenant implements Tenant {

    private final TenantKey tenantKey;

    public PlainTenant(TenantKey tenantKey) {
        this.tenantKey = Objects.requireNonNull(tenantKey, "tenantKey can't be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TenantKey getTenantKey() {
        return tenantKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSuper() {
        return tenantKey.isSuperTenant();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "tenantKey = " + tenantKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof PlainTenant) {
            PlainTenant other = PlainTenant.class.cast(obj);
            return Objects.equals(this.tenantKey, other.tenantKey);
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(tenantKey);
    }

}
