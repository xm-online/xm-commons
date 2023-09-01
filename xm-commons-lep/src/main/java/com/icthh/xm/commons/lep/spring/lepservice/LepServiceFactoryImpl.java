package com.icthh.xm.commons.lep.spring.lepservice;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.spring.ApplicationLepProcessingEvent;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.lep.api.ContextScopes;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.lep.api.LepProcessingEvent;
import com.icthh.xm.lep.api.ScopedContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

@LepService(group = "service.factory")
@Order(LOWEST_PRECEDENCE)
@IgnoreLogginAspect
@Slf4j
public class LepServiceFactoryImpl implements LepServiceFactory, RefreshableConfiguration,
        ApplicationListener<ApplicationLepProcessingEvent> {

    private static final String LEP_SERVICES = "lepServices";

    private final XmLepScriptConfigServerResourceLoader resourceLoader;
    private final TenantContextHolder tenantContextHolder;
    private final LepManager lepManager;
    private final Integer timeout;

    private final Map<String, Map<Class<?>, Object>> serviceInstances = new ConcurrentHashMap<>();
    private final Map<String, Map<Class<?>, Lock>> serviceLocks = new ConcurrentHashMap<>();

    private LepServiceFactoryImpl self;

    public LepServiceFactoryImpl(XmLepScriptConfigServerResourceLoader resourceLoader,
                                 TenantContextHolder tenantContextHolder,
                                 LepManager lepManager,
                                 @Value("${application.lep.service-factory-timeout:60}")
                                 Integer timeout) {
        this.resourceLoader = resourceLoader;
        this.tenantContextHolder = tenantContextHolder;
        this.lepManager = lepManager;
        this.timeout = timeout;
    }

    @Override
    @SneakyThrows
    public <T> T getInstance(Class<T> lepServiceClass) {
        String simpleClassName = lepServiceClass.getSimpleName();

        String tenantKey = tenantContextHolder.getTenantKey();
        Map<Class<?>, Object> tenantInstances = serviceInstances.computeIfAbsent(tenantKey, key -> new ConcurrentHashMap<>());
        var instance = tenantInstances.get(lepServiceClass);
        if (instance != null) {
            return (T) instance;
        }

        Map<Class<?>, Lock> tenantServiceFactoryLocks = serviceLocks.computeIfAbsent(tenantKey, key -> new ConcurrentHashMap<>());
        Lock lock = tenantServiceFactoryLocks.computeIfAbsent(lepServiceClass, key -> new ReentrantLock());
        log.trace("Try to acquired lock for service {}", lepServiceClass.getCanonicalName());
        StopWatch stopWatch = StopWatch.createStarted();
        if (!lock.tryLock(timeout, TimeUnit.SECONDS)) {
            throw new IllegalStateException(String.format("Timeout waiting service factory for service %s.", lepServiceClass.getCanonicalName()));
        }
        try {

            log.trace("Successfully acquired lock for service {} in {}ns", lepServiceClass.getSimpleName(), stopWatch.getNanoTime());

            instance = tenantInstances.get(lepServiceClass);
            if (instance != null) {
                return (T) instance;
            }

            var newInstance = self.createServiceByLepFactory(simpleClassName, lepServiceClass);
            tenantInstances.put(lepServiceClass, newInstance);
            return newInstance;
        } finally {
            lock.unlock();
            log.trace("Lock for service {} successfully released in {}ns", lepServiceClass.getSimpleName(), stopWatch.getNanoTime());
            tenantServiceFactoryLocks.remove(lepServiceClass);
        }
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

    @Autowired @Lazy
    public void setSelf(LepServiceFactoryImpl self) {
        this.self = self;
    }
}
