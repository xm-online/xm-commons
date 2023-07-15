package com.icthh.xm.commons.lep.api;

import com.icthh.xm.commons.lep.BaseProceedingLep;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.tenant.TenantContext;

public abstract class BaseLepContext implements Cloneable {

    public Object commons;
    public Object inArgs;
    public BaseProceedingLep lep;
    //public LepThreadHelper thread;
    public XmAuthenticationContext authContext;
    public TenantContext tenantContext;
    @Deprecated(forRemoval = true)
    public Object methodResult;

    //public LepServiceFactory lepServices;

    // TODO implement ScopedContext

    public void init() {
    }

    public final void baseInit() {

    }

}
