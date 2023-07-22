package com.icthh.xm.commons.lep.api;

import com.icthh.xm.commons.lep.BaseProceedingLep;
import com.icthh.xm.commons.lep.spring.LepThreadHelper;
import com.icthh.xm.commons.lep.spring.lepservice.LepServiceFactoryField;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.tenant.TenantContext;

import java.util.Map;

public abstract class BaseLepContext implements LepServiceFactoryField {

    public Object commons;
    public Object inArgs;
    public BaseProceedingLep lep;
    public LepThreadHelper thread;
    public XmAuthenticationContext authContext;
    public TenantContext tenantContext;
    @Deprecated(forRemoval = true)
    public Object methodResult;

    private Map<String, Object> additionalContext;

    public final Object get(String additionalContextKey) {
        return additionalContext.get(additionalContextKey);
    }

    public final void addAdditionalContext(String additionalContextKey, Object additionalContextValue) {
        additionalContext.put(additionalContextKey, additionalContextValue);
    }

}
