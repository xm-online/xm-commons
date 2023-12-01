package commons

import com.icthh.xm.commons.logging.util.MdcUtils
import com.icthh.xm.commons.tenant.TenantContext
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder
import com.icthh.xm.lep.api.ContextScopes
import com.icthh.xm.lep.api.ScopedContext
import com.icthh.xm.lep.core.CoreContextsHolder
import com.icthh.xm.lep.core.DefaultScopedContext
import org.springframework.security.core.context.SecurityContextHolder

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT


TenantContext tenantContext = lepContext.tenantContext
def context = lepContext.authContext
return new ThreadUtils(tenantContext,  context)

class ThreadUtils {
    TenantContext tenantContext
    def context

    ThreadUtils(tenantContext, context) {
        this.tenantContext = tenantContext
        this.context = context
    }

    def executeInNewContext(def operation) {
        ThreadLocal<ScopedContext> threadLocalContext = CoreContextsHolder.contexts.get(ContextScopes.THREAD)
        threadLocalContext.set(new DefaultScopedContext(ContextScopes.THREAD))
        def scopedContext = threadLocalContext.get()
        scopedContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContext);
        scopedContext.setValue(BINDING_KEY_AUTH_CONTEXT, context)
        new DefaultTenantContextHolder().getPrivilegedContext().setTenant(tenantContext.tenant.get())
        SecurityContextHolder.setContext(context.securityContext)

        def rid = MdcUtils.getRid()
        try {
            MdcUtils.putRid(rid)
            return operation.call()
        } finally {
            MdcUtils.removeRid()
            CoreContextsHolder.contexts.get(ContextScopes.THREAD)?.remove()
        }
    }
}
