package com.icthh.xm.lep.api;

import java.util.function.Consumer;

/**
 * @deprecated
 * Just remove it.
 * Lep engine will get TenantContext from TenantContextHolder and XmAuthenticationContext from XmAuthenticationContextHolder
 * User PrivilegedTenantContext to set current tenant and SecurityContextHolder to set current auth context.
 * To create new Thread pls use LepThreadHelper
 */
@Deprecated(forRemoval = true)
public interface LepManager extends LepManagerService {

    @Deprecated(forRemoval = true)
    void beginThreadContext(Consumer<? super ScopedContext> contextInitAction);

    @Deprecated(forRemoval = true)
    void endThreadContext();

}
