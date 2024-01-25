package com.icthh.xm.commons.lep.api;

import com.icthh.xm.commons.lep.BaseProceedingLep;
import com.icthh.xm.commons.lep.spring.LepThreadHelper;
import com.icthh.xm.commons.lep.spring.lepservice.LepServiceFactory;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.tenant.TenantContext;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

public abstract class BaseLepContext implements Map<String, Object> {

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

    private transient Map<String, Object> additionalContext = new HashMap<>();
    @Setter
    private transient LepContextMapSupport mapSupport;
    @Delegate(excludes = ExcludedMethods.class)
    private transient Map<String, Object> emptyMap = Map.of();

    public final Object get(Object fieldName) {
        if (mapSupport == null) { // fallback to reflection. to simplify groovy tests
            return ofNullable(additionalContext.get(fieldName)).orElseGet(() -> getFieldValue(fieldName));
        }
        return ofNullable(mapSupport.get(String.valueOf(fieldName), this)).orElse(additionalContext.get(fieldName));
    }

    @SneakyThrows
    private Object getFieldValue(Object fieldName) {
        return this.getClass().getField(String.valueOf(fieldName)).get(this);
    }

    public final void addAdditionalContext(String additionalContextKey, Object additionalContextValue) {
        additionalContext.put(additionalContextKey, additionalContextValue);
    }

    private interface ExcludedMethods {
        Object get(Object key);
    }

}
