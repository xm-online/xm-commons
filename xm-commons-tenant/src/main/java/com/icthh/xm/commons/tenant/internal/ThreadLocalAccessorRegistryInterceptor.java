package com.icthh.xm.commons.tenant.internal;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

@Slf4j
public class ThreadLocalAccessorRegistryInterceptor {

    /**
     * This method registers ThreadLocalAccessor in micrometer ContextRegistry. Ignored if micrometer is missing.
     * ContextRegistry is used for thread local transfer between threads in Project Reactor.
     * Please read <a href="https://github.com/micrometer-metrics/context-propagation">context-propagation library</a> for details.
     *
     * @param key the key to associate with the ThreadLocal value
     * @param threadLocal the underlying ThreadLocal
     */
    static void register(String key, ThreadLocal<TenantContextDataHolder> threadLocal) {
        try {
            Class<?> contextRegistry = Class.forName("io.micrometer.context.ContextRegistry");
            Method getInstance = contextRegistry.getMethod("getInstance");
            Method registerThreadLocalAccessor = contextRegistry.getMethod("registerThreadLocalAccessor", String.class, ThreadLocal.class);

            Object instance = getInstance.invoke(null);
            registerThreadLocalAccessor.invoke(instance, key, threadLocal);
            log.info("Successfully registered ThreadLocal in micrometer ContextRegistry by key '{}'", key);

        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError ignored) {
            log.error("Unexpected reflective exception while registering ThreadLocal in micrometer " +
                "ContextRegistry: {}", ignored.getMessage());
        } catch (Throwable err) {
            log.error("Unexpected exception while registering ThreadLocal in micrometer ContextRegistry. " +
                "The feature is considered disabled due to this:", err);
        }
    }
}
