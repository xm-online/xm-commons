package com.icthh.xm.lep.core;

import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.lep.api.ScopedContext;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.function.Consumer;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;

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

    private final CoreContextsHolder contextsHolder = new CoreContextsHolder();
    private final TenantContextHolder tenantContextHolder;

    @Override
    public void beginThreadContext(Consumer<? super ScopedContext> contextInitAction) {
        Objects.requireNonNull(contextInitAction, "context init action can't be null");
        ScopedContext scopedContext = contextsHolder.beginThreadContext();

        if (TenantContextUtils.getTenantKey(tenantContextHolder).isEmpty() && scopedContext.contains(THREAD_CONTEXT_KEY_TENANT_CONTEXT)) {
            TenantContext value = (TenantContext) scopedContext.getValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT);
            if (value != null && value.isInitialized() && value.getTenant().isPresent()) {
                tenantContextHolder.getPrivilegedContext().execute(value.getTenant().get(), () -> {
                    contextInitAction.accept(scopedContext);
                });
                return;
            }
        }

        contextInitAction.accept(scopedContext);
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
