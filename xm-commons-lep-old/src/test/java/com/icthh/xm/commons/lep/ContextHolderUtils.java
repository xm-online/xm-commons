package com.icthh.xm.commons.lep;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.lep.api.ContextScopes;
import com.icthh.xm.lep.api.ContextsHolder;
import com.icthh.xm.lep.api.ScopedContext;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantKey;
import org.mockito.Mockito;

import java.util.Optional;

/**
 * The {@link ContextHolderUtils} class.
 */
public final class ContextHolderUtils {

    public static ContextsHolder buildWithTenant(String tenantKey) {
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(tenantKey)));

        ScopedContext threadContext = mock(ScopedContext.class);
        Mockito.when(threadContext.getValue(eq(XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT),
                                            eq(TenantContext.class))).thenReturn(tenantContext);

        ContextsHolder contextsHolder = mock(ContextsHolder.class);
        Mockito.when(contextsHolder.getContext(eq(ContextScopes.THREAD))).thenReturn(threadContext);

        return contextsHolder;
    }

}
