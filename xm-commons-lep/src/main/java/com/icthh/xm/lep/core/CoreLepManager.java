package com.icthh.xm.lep.core;

import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.ContextScopes;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.lep.api.ScopedContext;
import lombok.RequiredArgsConstructor;

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
@RequiredArgsConstructor
public class CoreLepManager implements LepManager {

    private final TenantContextHolder tenantContextHolder;
    private final LepManagementService lepManagementService;

    @Override
    public void beginThreadContext(Consumer<? super ScopedContext> contextInitAction) {
        Objects.requireNonNull(contextInitAction, "context init action can't be null");
        ScopedContext scopedContext = new DefaultScopedContext(ContextScopes.THREAD);

        if (TenantContextUtils.getTenantKey(tenantContextHolder).isEmpty()) {
            throw new IllegalStateException("Thread context not initd");
        }

        contextInitAction.accept(scopedContext);
        lepManagementService.beginThreadContext();
    }

    @Override
    public void endThreadContext() {
        lepManagementService.endThreadContext();
    }

}
