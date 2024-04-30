package com.icthh.xm.commons.lep.spring.lepservice;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@IgnoreLogginAspect
@LepService(group = "service.factory")
public class LepServiceFactoryWithLepFactoryMethod {

    private final Integer timeout;

    private final Map<String, Map<String, Class<?>>> classCache = new ConcurrentHashMap<>();
    private final Map<String, Map<Class<?>, Object>> serviceInstances = new ConcurrentHashMap<>();
    private final Map<String, Map<Class<?>, Lock>> serviceLocks = new ConcurrentHashMap<>();

    private LepServiceFactoryWithLepFactoryMethod self;

    public LepServiceFactoryWithLepFactoryMethod(@Value("${application.lep.service-factory-timeout:60}")
                                                 Integer timeout) {
        this.timeout = timeout;
    }

    @SneakyThrows
    public <T> T getInstance(String scopeId, Class<T> lepServiceClass) {
        String simpleClassName = lepServiceClass.getSimpleName();

        Map<Class<?>, Object> tenantInstances = serviceInstances.computeIfAbsent(scopeId, key -> new ConcurrentHashMap<>());
        var instance = tenantInstances.get(lepServiceClass);
        if (instance != null) {
            return (T) instance;
        }

        Map<Class<?>, Lock> tenantServiceFactoryLocks = serviceLocks.computeIfAbsent(scopeId, key -> new ConcurrentHashMap<>());
        Lock lock = tenantServiceFactoryLocks.computeIfAbsent(lepServiceClass, key -> new ReentrantLock());
        StopWatch stopWatch = StopWatch.createStarted();
        log.trace("Try to acquired lock for service {}", lepServiceClass.getCanonicalName());
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

    @Autowired
    public void setSelf(LepServiceFactoryWithLepFactoryMethod self) {
        this.self = self;
    }

    public void clear(String scopeId) {
        classCache.remove(scopeId);
        serviceInstances.remove(scopeId);
        serviceLocks.remove(scopeId);
    }

    @LogicExtensionPoint(value = "ClassForNameResolver")
    public <T> Class<T> classForNameResolver(String className) {
        // Exception will never happen
        throw new RuntimeException("Error with class resolving for class with name" + className);
    }

    public <T> T getInstance(String scopeId, String lepClassName) {
        Map<String, Class<?>> tenantClasses = classCache.computeIfAbsent(scopeId, key -> new ConcurrentHashMap<>());
        Class<T> lepClass = (Class<T>) tenantClasses.computeIfAbsent(lepClassName, key -> self.classForNameResolver(lepClassName));
        return getInstance(scopeId, lepClass);
    }
}
