package com.icthh.xm.lep.core;

import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.lep.api.ScopedContext;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @deprecated
 * Use LepManagementService
 * try (var context = lepManagementService.beginThreadContext()) {
 *     // run lep`s
 * }
 * Lep engine will get TenantContext from TenantContextHolder and XmAuthenticationContext from XmAuthenticationContextHolder
 * User PrivilegedTenantContext to set current tenant and SecurityContextHolder to set current auth context.
 * To create new Thread pls use LepThreadHelper
 */
@Deprecated(forRemoval = true)
public class CoreLepManager implements LepManager {

    private final CoreContextsHolder contextsHolder = new CoreContextsHolder();

    @Override
    public void beginThreadContext(Consumer<? super ScopedContext> contextInitAction) {
        Objects.requireNonNull(contextInitAction, "context init action can't be null");
        contextInitAction.accept(contextsHolder.beginThreadContext());
    }

    @Override
    public void endThreadContext() {
        contextsHolder.endThreadContext();
    }

    @Override
    public ScopedContext getContext(String scope) {
        return contextsHolder.getContext(scope);
    }

}
