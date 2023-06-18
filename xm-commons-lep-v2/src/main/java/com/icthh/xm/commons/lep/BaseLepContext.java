package com.icthh.xm.commons.lep;

import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.tenant.TenantContext;

public abstract class BaseLepContext {

    public Object commons;
    public Object inArgs;
    public BaseProceedingLep lep;
    //public LepThreadHelper thread;
    //public TraceService traceService;
    //public OutboxTransportService outboxTransportService;
    public XmAuthenticationContext authContext;
    public TenantContext tenantContext;
    public Object methodResult;

    //public LepServiceFactory lepServices;
}
