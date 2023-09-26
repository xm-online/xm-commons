package com.icthh.xm.commons.lep.api;

import com.icthh.xm.commons.lep.BaseProceedingLep;
import com.icthh.xm.commons.lep.spring.LepThreadHelper;
import com.icthh.xm.commons.lep.spring.lepservice.LepServiceFactory;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.tenant.TenantContext;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseLepContext {

    public Object commons;
    public Object inArgs;
    public BaseProceedingLep lep;
    public LepThreadHelper thread;
    public XmAuthenticationContext authContext;
    public TenantContext tenantContext;
    // just remove usage, no alternatives
    @Deprecated(forRemoval = true)
    public Object methodResult;
    public LepServiceFactory lepServices;

    private Map<String, Object> additionalContext = new HashMap<>();

    public final Object get(String additionalContextKey) {
        return additionalContext.get(additionalContextKey);
    }

    public final void addAdditionalContext(String additionalContextKey, Object additionalContextValue) {
        additionalContext.put(additionalContextKey, additionalContextValue);
    }

}
