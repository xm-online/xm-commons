package com.icthh.xm.commons.lep;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;

import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.lep.api.ContextScopes;
import com.icthh.xm.lep.api.ContextsHolder;
import com.icthh.xm.lep.api.ScopedContext;
import lombok.experimental.UtilityClass;

/**
 * The {@link LepContextUtils} class.
 */
@UtilityClass
@SuppressWarnings("squid:S1118") // private constructor generated by lombok
public final class LepContextUtils {

    public static String getTenantKey(ContextsHolder lepContextsHolder) {
        ScopedContext scopedContext = getThreadContext(lepContextsHolder);
        TenantContext tenantContext = scopedContext.getValue(XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT,
                                                             TenantContext.class);
        if (tenantContext == null) {
            throw new IllegalStateException(
                "LEP manager thread context doesn't have value for var: "
                    + XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT);
        }
        return getRequiredTenantKeyValue(tenantContext);
    }

    private static ScopedContext getThreadContext(ContextsHolder lepContextsHolder) {
        ScopedContext scopedContext = lepContextsHolder.getContext(ContextScopes.THREAD);
        if (scopedContext == null) {
            throw new IllegalStateException("LEP manager thread context doesn't initialized. "
                                                + "Be sure that LepManager.beginThreadContext is called");
        }
        return scopedContext;
    }

}