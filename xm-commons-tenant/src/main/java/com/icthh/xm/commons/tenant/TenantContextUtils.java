package com.icthh.xm.commons.tenant;

import java.util.Optional;
import java.util.regex.Pattern;
import com.icthh.xm.commons.exceptions.BusinessException;

/**
 * The {@link TenantContextUtils} class.
 */
public final class TenantContextUtils {

    public static String TENANT_KEY_REGEXP = "^(?!pg_)(?!_)[a-zA-Z][a-zA-Z0-9_]{0,47}$";
    public static Pattern TENANT_KEY_PATTERN = Pattern.compile(TENANT_KEY_REGEXP);
    public static final String TENANT_KEY_FORMAT_CODE = "xm.xmEntity.tenant.error.tenantKeyFormat";

    /**
     * Get tenant key value by holder.
     *
     * @param holder tenant context's holder
     * @return tenant key for current thread
     */
    public static String getRequiredTenantKeyValue(TenantContextHolder holder) {
        return getRequiredTenantKeyValue(holder.getContext());
    }

    /**
     * Get tenant key value by context.
     *
     * @param context tenant context
     * @return tenant key for current thread
     */
    public static String getRequiredTenantKeyValue(TenantContext context) {
        return getRequiredTenantKey(context).getValue();
    }

    /**
     * Get tenant key object by holder.
     *
     * @param holder tenant context's holder
     * @return tenant key object for current thread
     */
    public static TenantKey getRequiredTenantKey(TenantContextHolder holder) {
        return getRequiredTenantKey(holder.getContext());
    }

    /**
     * Get tenant key object by context.
     *
     * @param context tenant context
     * @return tenant key object for current thread
     */
    public static TenantKey getRequiredTenantKey(TenantContext context) {
        return context.getTenantKey()
            .orElseThrow(() -> new IllegalStateException("Tenant context doesn't have tenant key"));
    }

    /**
     * Get tenant key optional object by holder.
     *
     * @param holder tenant context's holder
     * @return optional tenant key for current thread
     */
    public static Optional<TenantKey> getTenantKey(TenantContextHolder holder) {
        return holder.getContext().getTenantKey();
    }

    /**
     * Sets current thread tenant.
     *
     * @param holder         tenant contexts holder
     * @param tenantKeyValue tenant key value
     */
    public static void setTenant(TenantContextHolder holder, String tenantKeyValue) {
        holder.getPrivilegedContext().setTenant(buildTenant(tenantKeyValue));
    }

    /**
     * Sets current thread tenant.
     *
     * @param holder    tenant contexts holder
     * @param tenantKey tenant key
     */
    public static void setTenant(TenantContextHolder holder, TenantKey tenantKey) {
        holder.getPrivilegedContext().setTenant(buildTenant(tenantKey));
    }

    /**
     * Build {@link Tenant} instance by key value.
     *
     * @param tenantKeyValue tenant key value
     * @return instance of {@link Tenant}
     */
    public static Tenant buildTenant(String tenantKeyValue) {
        return buildTenant(TenantKey.valueOf(tenantKeyValue));
    }

    /**
     * Build {@link Tenant} instance by key.
     *
     * @param tenantKey tenant key
     * @return instance of {@link Tenant}
     */
    public static Tenant buildTenant(TenantKey tenantKey) {
        return new PlainTenant(tenantKey);
    }

    public static boolean isTenantKeyValid(String tenantKey) {
        return TENANT_KEY_PATTERN.matcher(tenantKey).matches();
    }

    public static void assertTenantKeyValid(String tenantKey) {
        if (!isTenantKeyValid(tenantKey)) {
            throw new BusinessException(TENANT_KEY_FORMAT_CODE, "Tenant key wrong format");
        }
    }

    /**
     * Private utils class constructor.
     */
    private TenantContextUtils() {
        throw new IllegalAccessError("access not allowed");
    }

}
