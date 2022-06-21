package com.icthh.xm.commons.lep.spring.lepservice;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.spring.ApplicationLepProcessingEvent;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.lep.api.ContextScopes;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.lep.api.LepProcessingEvent;
import com.icthh.xm.lep.api.ScopedContext;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

@LepService(group = "service.factory")
@Order(LOWEST_PRECEDENCE)
public class LepServiceFactoryImpl implements LepServiceFactory, RefreshableConfiguration,
        ApplicationListener<ApplicationLepProcessingEvent> {

    private static final String LEP_SERVICES = "lepServices";

    private final XmLepScriptConfigServerResourceLoader resourceLoader;
    private final TenantContextHolder tenantContextHolder;
    private final LepManager lepManager;

    private final Map<String, Map<Class<?>, Object>> serviceInstances = new ConcurrentHashMap<>();

    private LepServiceFactoryImpl self;

    public LepServiceFactoryImpl(XmLepScriptConfigServerResourceLoader resourceLoader,
                                 TenantContextHolder tenantContextHolder,
                                 LepManager lepManager) {
        this.resourceLoader = resourceLoader;
        this.tenantContextHolder = tenantContextHolder;
        this.lepManager = lepManager;
    }

    @Override
    public <T> T getInstance(Class<T> lepServiceClass) {
        String simpleClassName = lepServiceClass.getSimpleName();

        String tenantKey = tenantContextHolder.getTenantKey();
        Map<Class<?>, Object> tenantInstances = serviceInstances.computeIfAbsent(tenantKey, key -> new ConcurrentHashMap<>());
        return (T) tenantInstances.computeIfAbsent(lepServiceClass, key -> {
            return self.createServiceByLepFactory(simpleClassName, lepServiceClass);
        });
    }

    @LogicExtensionPoint(value = "ServiceFactory", resolver = LepServiceFactoryResolver.class)
    public <T> T createServiceByLepFactory(String serviceClassName, Class<T> type) {
        return self.createServiceByGeneratedLepFactory(serviceClassName, type);
    }

    @LogicExtensionPoint(value = "GeneratedServiceFactory")
    public <T> T createServiceByGeneratedLepFactory(String serviceClassName, Class<T> type) {
        // Exception will never happen
        throw new RuntimeException("Error with service factory generation " + serviceClassName);
    }

    @Override
    public void onApplicationEvent(ApplicationLepProcessingEvent event) {
        if (event.getLepProcessingEvent() instanceof LepProcessingEvent.BeforeExecutionEvent) {
            ScopedContext context = lepManager.getContext(ContextScopes.EXECUTION);
            context.setValue(LEP_SERVICES, this);
        }
    }

    @Override
    public void refreshFinished(Collection<String> paths) {
        serviceInstances.clear();
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return resourceLoader.isListeningConfiguration(updatedKey);
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        // Do nothing
    }

    @Autowired
    public void setSelf(LepServiceFactoryImpl self) {
        this.self = self;
    }
}