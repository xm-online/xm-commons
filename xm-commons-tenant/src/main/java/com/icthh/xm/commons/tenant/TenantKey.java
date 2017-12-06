package com.icthh.xm.commons.tenant;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * The {@link TenantKey} class.
 */
public class TenantKey {

    /**
     * Super tenant key value.
     */
    public static final String SUPER_TENANT_KEY_VALUE = "xm";

    /**
     * Super tenant key.
     */
    public static final TenantKey SUPER = TenantKey.valueOf(SUPER_TENANT_KEY_VALUE);

    /**
     * Unique alphabetical tenant key value.
     */
    private final String value;

    public TenantKey(String value) {
        this.value = Objects.requireNonNull(value, "value can't be null");
    }

    /**
     * Static {@link TenantKey} builder.
     *
     * @param tenantKey unique tenant key value
     * @return the {@link TenantKey} object
     */
    public static TenantKey valueOf(String tenantKey) {
        return new TenantKey(tenantKey);
    }

    /**
     * Gets unique tenant key value.
     *
     * @return the tenant key value
     */
    public String getValue() {
        return value;
    }

    public boolean isSuperTenant() {
        return SUPER_TENANT_KEY_VALUE.equalsIgnoreCase(getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "value = " + value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof TenantKey) {
            TenantKey other = TenantKey.class.cast(obj);
            return StringUtils.equalsIgnoreCase(this.value, other.value);
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

}
