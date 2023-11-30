package com.icthh.xm.lep.core;

import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.lep.api.ContextScopes;
import com.icthh.xm.lep.api.ContextsHolder;
import com.icthh.xm.lep.api.ScopedContext;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.impl.internal.MigrationFromCoreContextsHolderLepManagementServiceReference.getLepManagementServiceInstance;

/**
 * @deprecated
 * Just remove it.
 * Lep engine will get TenantContext from TenantContextHolder and XmAuthenticationContext from XmAuthenticationContextHolder
 * User PrivilegedTenantContext to set current tenant and SecurityContextHolder to set current auth context.
 * To create new Thread pls use LepThreadHelper
 */
@Deprecated(forRemoval = true)
public class CoreContextsHolder implements ContextsHolder {

    private static volatile Map<String, ThreadLocal<ScopedContext>> contexts = new HashMap<>();

    static {
        // TODO put thread local proxy that init and end context
        contexts.put(ContextScopes.THREAD, new MigrationBridgeThreadLocalContext());
        contexts.put(ContextScopes.EXECUTION, new ThreadLocal<>());
    }

    private static ThreadLocal<ScopedContext> getThreadLocalContext(String scope) {
        return contexts.get(scope);
    }

    @Override
    public ScopedContext getContext(String scope) {
        Objects.requireNonNull(scope, "scope can't be null");

        ThreadLocal<ScopedContext> threadLocalContext = getThreadLocalContext(scope);
        if (threadLocalContext == null) {
            throw new IllegalArgumentException("Unsupported context scope name: " + scope);
        }

        return threadLocalContext.get();
    }

    private ScopedContext beginContext(String scope) {
        ThreadLocal<ScopedContext> threadLocalContext = getThreadLocalContext(scope);
        threadLocalContext.set(new DefaultScopedContext(scope));
        return threadLocalContext.get();
    }

    private void endContext(String scope) {
        ThreadLocal<ScopedContext> threadLocalContext = getThreadLocalContext(scope);
        threadLocalContext.remove();
    }

    public ScopedContext beginThreadContext() {
        return beginContext(ContextScopes.THREAD);
    }

    public void endThreadContext() {
        endContext(ContextScopes.THREAD);
    }

    public static class MigrationBridgeThreadLocalContext extends ThreadLocal<ScopedContext> {

        @Override
        public void set(ScopedContext value) {
            super.set(value);
            if (CoreContextsHolder.isTenantContextPresent()) {
                getLepManagementServiceInstance().beginThreadContext();
            }
        }

        public ScopedContext get() {
            ScopedContext scopedContext = super.get();
            if (scopedContext == null) {
                return scopedContext;
            }

            return new MigrationScopedContextBridge(scopedContext);
        }

        @Override
        public void remove() {
            super.remove();
            getLepManagementServiceInstance().endThreadContext();
        }
    }

    private static boolean isTenantContextPresent() {
        return new DefaultTenantContextHolder().getPrivilegedContext().getTenantKey().isPresent();
    }

    @RequiredArgsConstructor
    public static class MigrationScopedContextBridge implements ScopedContext {

        private final ScopedContext scopedContext;

        @Override
        public String getScope() {
            return ContextScopes.THREAD;
        }

        @Override
        public Set<String> getNames() {
            return scopedContext.getNames();
        }

        @Override
        public boolean contains(String name) {
            return scopedContext.contains(name);
        }

        @Override
        public Object getValue(String key) {
            return scopedContext.getValue(key);
        }

        @Override
        public <T> T getValue(String name, Class<T> castToType) {
            return scopedContext.getValue(name, castToType);
        }

        @Override
        public void setValue(String key, Object value) {
            scopedContext.setValue(key, value);
            if (!CoreContextsHolder.isTenantContextPresent() && key.equals(THREAD_CONTEXT_KEY_TENANT_CONTEXT) && value instanceof TenantContext tenantContext) {
                new DefaultTenantContextHolder().getPrivilegedContext().setTenant(tenantContext.getTenant().get());
                getLepManagementServiceInstance().beginThreadContext();
            }
        }

        @Override
        public Map<String, Object> getValues() {
            return scopedContext.getValues();
        }


    };

}
