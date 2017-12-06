package com.icthh.xm.commons.tenant.internal;

import com.icthh.xm.commons.tenant.Tenant;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The {@link TenantContextDataHolder} class.
 */
final class TenantContextDataHolder {

    /**
     * Stores the current TenantContext local to the running thread.
     */
    private static ThreadLocal<TenantContextDataHolder> holderInstance = ThreadLocal
        .withInitial(TenantContextDataHolder::new);

    /**
     * Stores references to the existing TenantContexts when starting tenant flows. These references
     * will be popped back, when a tenant flow is ended.
     */
    private static ThreadLocal<Stack<TenantContextDataHolder>> parentHolderInstanceStack =
        ThreadLocal.withInitial(Stack::new);

    /**
     * Holder for tenant instance.
     */
    private ValueHolder<Tenant> tenant = ValueHolder.empty();

    /**
     * Holder for data instance.
     */
    private ValueHolder<Map<String, Object>> dataMap = ValueHolder.empty();

    /**
     * Default constructor to disallow creation of the TenantContextDataHolder.
     */
    private TenantContextDataHolder() {
        super();
    }

    /**
     * This method will always attempt to obtain an instance of the current TenantContextDataHolder from
     * the thread-local copy.
     *
     * @return the {@link TenantContextDataHolder} holder.
     */
    static TenantContextDataHolder getThreadLocalInstance() {
        return holderInstance.get();
    }

    private boolean isAllowedToChangeTenant(StackTraceElement[] traces) {
        // this is current fake implementation that always true
        // need more strict security call stack check
        return (traces != null) && traces.length > 0;
    }

    Optional<Tenant> getTenant() {
        return tenant.isPresent() ? Optional.of(tenant.get()) : Optional.empty();
    }

    void setTenant(Tenant tenant) {
        Objects.requireNonNull(tenant, "tenant can't be null");

        // DO NOT CHANGE !!!!
        // allow tenant change only from initial (empty) value or only from super tenant value
        if (this.tenant.isEmpty() || this.tenant.get().isSuper()) {
            this.tenant = ValueHolder.valueOf(tenant);
        } else if (!Objects.equals(this.tenant.get(), tenant)) {
            StackTraceElement[] traces = Thread.currentThread().getStackTrace();
            if (!isAllowedToChangeTenant(traces)) {
                throw new IllegalStateException("Trying to set the tenant from " + this.tenant + " to " + tenant);
            }
        }
    }

    private Map<String, Object> getRequiredData() {
        return dataMap.orElseThrow((Supplier<RuntimeException>) () ->
            new IllegalStateException("data instance not initialized in TenantContext"));
    }

    Object getDataValue(String key) {
        return getRequiredData().get(key);
    }

    Map<String, Object> getData() {
        return new HashMap<>(getRequiredData());
    }

    /**
     * Sets tenant context data. Must be called after {@link #setTenant}.
     *
     * @param data data to set into context
     */
    void setData(Map<String, Object> data) {
        Objects.requireNonNull(data, "data can't be null");

        // @adovbnya DO NOT CHANGE !!!!
        // allow tenant change only from initial (empty) value or only from super tenant
        if (this.dataMap.isEmpty()) {
            this.dataMap = ValueHolder.valueOf(data);
        } else if (!Objects.equals(this.dataMap.get(), data)) {
            if (!this.tenant.isPresent()) {
                throw new IllegalStateException("Tenant doesn't set in context yet");
            }

            if (this.tenant.get().isSuper()) {
                this.dataMap = ValueHolder.valueOf(data);
            } else {

                StackTraceElement[] traces = Thread.currentThread().getStackTrace();
                if (!isAllowedToChangeTenant(traces)) {
                    throw new IllegalStateException("Trying to set the data from " + this.dataMap.get()
                                                        + " to " + data);
                }
            }
        }
    }

    /**
     * Starts a tenant flow. This will stack the current TenantContext and begin a new nested flow
     * which can have an entirely different context. This is ideal for scenarios where multiple
     * super-tenant and sub-tenant phases are required within as a single block of execution.
     * This method starts a new tenant flow, and creates a new holder for tenant data.
     * Thereafter, until endTenantFlow is called, the getTenantContext() methods will return the data related
     * to the newly created tenant data holder.
     * Once startTenantFlow is called, set the tenant itself and tenant specific data.
     * See TenantContextDataHolder#startTenantFlow()
     */
    private static void startTenantFlow() {
        Stack<TenantContextDataHolder> contextDataStack = parentHolderInstanceStack.get();
        contextDataStack.push(holderInstance.get());
        holderInstance.remove();
    }

    /**
     * This will end the tenant flow and restore the previous TenantContext.
     * See TenantContextDataHolder#endTenantFlow()
     */
    private static void endTenantFlow() {
        Stack<TenantContextDataHolder> contextDataStack = parentHolderInstanceStack.get();
        if (contextDataStack != null) {
            holderInstance.set(contextDataStack.pop());
        }
    }

    /**
     * This method will destroy the current TenantContext data holder.
     */
    void destroyForCurrentThread() {
        holderInstance.remove();
        parentHolderInstanceStack.remove();
    }

    boolean isInitialized() {
        return !tenant.isEmpty();
    }

    /**
     * This method will execute {@code supplier.getValue()} method on behalf of the specified tenant and
     * will return the result of the called supplier.
     *
     * @param tenant   the tenant
     * @param supplier an instance on which {@code getValue()} method will be executed
     * @param <V>      return value type can be {@link java.lang.Void}
     * @return the result of {@code supplier.getValue()} execution
     */
    static <V> V execute(Tenant tenant, Supplier<V> supplier) {
        startTenantFlow();
        try {
            getThreadLocalInstance().setTenant(tenant);
            return supplier.get();
        } finally {
            endTenantFlow();
        }
    }

    /**
     * This method will execute {@code runnable.run()} method on behalf of the specified tenant.
     *
     * @param tenant   the tenant
     * @param runnable an instance on which {@code run()} method will be executed
     */
    static void execute(Tenant tenant, Runnable runnable) {
        startTenantFlow();
        try {
            getThreadLocalInstance().setTenant(tenant);
            runnable.run();
        } finally {
            endTenantFlow();
        }
    }

    /**
     * This method will execute {@code consumer.accept(T value)} method on behalf of the specified tenant.
     *
     * @param tenant   the tenant
     * @param consumer an instance on which {@code accept(T value)} method will be executed
     * @param value    the input argument for consumer
     * @param <V>      the type of the input to the consumer operation
     */
    static <V> void execute(Tenant tenant, Consumer<V> consumer, V value) {
        startTenantFlow();
        try {
            getThreadLocalInstance().setTenant(tenant);
            consumer.accept(value);
        } finally {
            endTenantFlow();
        }
    }

}
