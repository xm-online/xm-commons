package com.icthh.xm.commons.tenant;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The {@link PrivilegedTenantContext} interface.
 */
public interface PrivilegedTenantContext extends TenantContext {

    /**
     * Sets tenant object.
     *
     * @param tenant the tenant object
     */
    void setTenant(Tenant tenant);

    /**
     * Destroys the current ThreadLocal TenantContext.
     * Need to be called for each thread where used {@link TenantContext}.
     */
    void destroyCurrentContext();

    // Tenant should be validated to ensure that the tenant exist and is allowed to be used by the current user
    // and on this VM.

    /**
     * This method will execute {@code supplier.get()} method on behalf of the specified tenant and
     * will return the result of the called supplier.
     *
     * @param tenant   the tenant
     * @param supplier an instance on which {@code get()} method will be executed
     * @param <V>      return value type can be {@link java.lang.Void}
     * @return the result of {@code supplier.get()} execution
     */
    <V> V execute(Tenant tenant, Supplier<V> supplier);

    /**
     * This method will execute {@code runnable.run()} method on behalf of the specified tenant.
     *
     * @param tenant   the tenant
     * @param runnable an instance on which {@code run()} method will be executed
     */
    void execute(Tenant tenant, Runnable runnable);

    /**
     * This method will execute {@code consumer.accept(T value)} method on behalf of the specified tenant.
     *
     * @param tenant   the tenant
     * @param consumer an instance on which {@code accept(T value)} method will be executed
     * @param value    the input argument for consumer
     * @param <V>      the type of the input to the consumer operation
     */
    <V> void execute(Tenant tenant, Consumer<V> consumer, V value);


}
