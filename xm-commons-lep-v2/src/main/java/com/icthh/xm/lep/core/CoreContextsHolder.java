package com.icthh.xm.lep.core;

import com.icthh.xm.lep.api.ContextScopes;
import com.icthh.xm.lep.api.ContextsHolder;
import com.icthh.xm.lep.api.ScopedContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
        contexts.put(ContextScopes.THREAD, new ThreadLocal<>());
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

}
