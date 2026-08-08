package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.api.BaseLepContext;
import groovy.lang.GString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.codehaus.groovy.runtime.GStringImpl;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.springframework.beans.factory.BeanClassLoaderAware;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Once-per-JVM warmup of the groovy runtime state that is shared between lep engines and cannot be warmed
 * per engine: metaclasses of the receiver classes every lep execution touches (the actual LepContext class
 * together with the object graph of its fields, GString and the common JDK types).
 *
 * <p>Why this exists. Creating a metaclass runs {@code Introspector.getBeanInfo}, which probes class names
 * that can never exist ({@code <Class>BeanInfo}, {@code <Class>Customizer},
 * {@code groovy.runtime.metaclass.*}). The engine warmup only initializes classes of the lep classloader,
 * so the receiver metaclasses are created lazily by the first lep execution - measured on a real service
 * that made the first request after a restart take over a minute in a packed WAR, where every classloader
 * miss of a request thread is resolved by Tomcat streaming nested jars (see
 * {@code TomcatBloomArchiveIndexConfiguration} in xm-commons-ms-web for the container side of the fix).
 *
 * <p>The warmup runs on a background daemon thread started by the {@link Starter} bean as soon as the bean
 * classloader is known, i.e. concurrently with the rest of spring startup. Lep traffic cannot observe a
 * partially warmed runtime: the {@code GroovyLepEngine} script warmup calls {@link #awaitCompletion()}
 * before the "lep engines inited" latch opens.
 */
@Slf4j
public final class GroovySharedRuntimeWarmup {

    /**
     * A name that never resolves: the lookup pays the classloader miss cost (jar scanning in a packed WAR)
     * once on the warmup thread, so the first metaclass initialization does not pay it inside a request.
     */
    static final String MISS_PROBE_CLASS_NAME =
        "groovy.runtime.metaclass.GroovySharedRuntimeWarmupMissProbeMetaClass";

    private static final int MAX_FIELD_GRAPH_DEPTH = 3;
    private static final int MAX_RECEIVER_CLASSES = 1000;

    private static final List<Class<?>> CORE_RECEIVER_CLASSES = List.of(
        GString.class, GStringImpl.class, String.class, Boolean.class, Integer.class, Long.class,
        Double.class, BigDecimal.class, ArrayList.class, LinkedHashMap.class, HashMap.class,
        LinkedHashSet.class, List.class, Map.class, Set.class, Object.class
    );

    /**
     * Upper bound for {@link #awaitCompletion()}: far above any realistic warmup duration, present only so
     * a pathologically hanging filesystem cannot block the lep engine init forever.
     */
    private static final long AWAIT_COMPLETION_TIMEOUT_MINUTES = 15;

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static volatile CountDownLatch done = new CountDownLatch(1);

    private GroovySharedRuntimeWarmup() {
    }

    /**
     * Declared as a bean by {@code GroovyLepEngineConfiguration} when lep warmup is enabled. Spring hands
     * every bean the application classloader early in the startup, which is the trigger to run the warmup
     * in the background - no other lifecycle is required, so the bean has no methods of its own.
     */
    public static class Starter implements BeanClassLoaderAware {

        private final Supplier<Class<? extends BaseLepContext>> lepContextClass;

        public Starter(Supplier<Class<? extends BaseLepContext>> lepContextClass) {
            this.lepContextClass = lepContextClass;
        }

        @Override
        public void setBeanClassLoader(ClassLoader classLoader) {
            warmupOnceAsync(classLoader, lepContextClass);
        }
    }

    /**
     * Starts the warmup on a daemon thread. Only the first call of the JVM has an effect: the state warmed
     * here (metaclasses of application classes) is process wide and survives lep engine recreation.
     *
     * @param classLoader     the application classloader
     * @param lepContextClass resolves the actual LepContext class of the microservice, may be null
     * @return whether this call started the warmup
     */
    public static boolean warmupOnceAsync(ClassLoader classLoader,
                                          Supplier<Class<? extends BaseLepContext>> lepContextClass) {
        if (classLoader == null || !STARTED.compareAndSet(false, true)) {
            return false;
        }
        Thread thread = new Thread(() -> warmup(classLoader, lepContextClass), "lep-shared-runtime-warmup");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    /**
     * Blocks until the warmup started by {@link #warmupOnceAsync} finishes; returns immediately when no
     * warmup was ever started. Called at the end of the per-tenant lep warmup, which runs before the
     * "lep engines inited" latch opens - so lep traffic never races the shared warmup, while the web
     * container readiness is not delayed.
     */
    public static void awaitCompletion() {
        if (!STARTED.get()) {
            return;
        }
        try {
            if (!done.await(AWAIT_COMPLETION_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                log.warn("Lep shared runtime warmup did not finish within {} minutes, continuing without it",
                    AWAIT_COMPLETION_TIMEOUT_MINUTES);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for lep shared runtime warmup");
        }
    }

    static void warmup(ClassLoader classLoader, Supplier<Class<? extends BaseLepContext>> lepContextClass) {
        try {
            StopWatch stopWatch = StopWatch.createStarted();
            sweepClassLoaderMiss(classLoader);
            long sweepTime = stopWatch.getTime(MILLISECONDS);
            Set<Class<?>> warmed = warmupReceiverMetaClasses(lepContextClass);
            log.info("Lep shared runtime warmup done: classloader miss sweep {} ms, {} receiver metaclasses, total {} ms",
                sweepTime, warmed.size(), stopWatch.getTime(MILLISECONDS));
        } finally {
            done.countDown();
        }
    }

    private static void sweepClassLoaderMiss(ClassLoader classLoader) {
        try {
            Class.forName(MISS_PROBE_CLASS_NAME, false, classLoader);
            log.warn("Classloader miss probe unexpectedly resolved {}", MISS_PROBE_CLASS_NAME);
        } catch (ClassNotFoundException expected) {
            log.debug("Classloader miss probe swept {}", classLoader);
        } catch (Throwable e) {
            log.warn("Classloader miss sweep failed: {}", e.toString());
        }
    }

    private static Set<Class<?>> warmupReceiverMetaClasses(Supplier<Class<? extends BaseLepContext>> lepContextClass) {
        Set<Class<?>> receivers = collectReceiverClasses(lepContextClass);
        for (Class<?> receiver : receivers) {
            try {
                InvokerHelper.getMetaClass(receiver);
            } catch (Throwable e) {
                log.debug("Error warmup metaclass of {}: {}", receiver.getName(), e.toString());
            }
        }
        warmupGStringRuntime();
        return receivers;
    }

    static Set<Class<?>> collectReceiverClasses(Supplier<Class<? extends BaseLepContext>> lepContextClass) {
        Set<Class<?>> visited = new LinkedHashSet<>(CORE_RECEIVER_CLASSES);
        Class<?> actualLepContextClass = resolveLepContextClass(lepContextClass);
        if (actualLepContextClass != null) {
            collectFieldGraph(actualLepContextClass, visited);
        }
        return visited;
    }

    private static Class<?> resolveLepContextClass(Supplier<Class<? extends BaseLepContext>> lepContextClass) {
        try {
            return lepContextClass != null ? lepContextClass.get() : null;
        } catch (Throwable e) {
            log.warn("Error detect actual lep context class: {}", e.toString());
            return null;
        }
    }

    /**
     * Walks the public instance fields of the lep context class graph: {@code lepContext.<field>.<field>}
     * is exactly how every lep reaches its services, so the field types are the receivers whose metaclasses
     * the first execution would otherwise create. JDK types are warmed but not recursed into.
     */
    private static void collectFieldGraph(Class<?> root, Set<Class<?>> visited) {
        Deque<ClassAtDepth> queue = new ArrayDeque<>();
        queue.add(new ClassAtDepth(root, 0));
        while (!queue.isEmpty() && visited.size() < MAX_RECEIVER_CLASSES) {
            ClassAtDepth current = queue.poll();
            Class<?> clazz = unwrap(current.clazz());
            if (clazz == null || clazz.isPrimitive() || !visited.add(clazz)) {
                continue;
            }
            if (current.depth() >= MAX_FIELD_GRAPH_DEPTH || clazz.getName().startsWith("java.")) {
                continue;
            }
            for (Class<?> type = clazz; type != null && type != Object.class; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (Modifier.isPublic(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
                        queue.add(new ClassAtDepth(field.getType(), current.depth() + 1));
                    }
                }
            }
        }
    }

    private static Class<?> unwrap(Class<?> clazz) {
        return clazz != null && clazz.isArray() ? unwrap(clazz.getComponentType()) : clazz;
    }

    /**
     * Runs the exact code path of a groovy string interpolation once: GString static init plus the
     * metaclass of its runtime anonymous subclass, both created lazily on the first {@code "${...}"} of the
     * JVM otherwise.
     */
    private static void warmupGStringRuntime() {
        try {
            new GStringImpl(new Object[]{Integer.valueOf(0)}, new String[]{"", ""}).toString();
        } catch (Throwable e) {
            log.debug("Error warmup GString runtime: {}", e.toString());
        }
    }

    static void resetForTests() {
        STARTED.set(false);
        done = new CountDownLatch(1);
    }

    private record ClassAtDepth(Class<?> clazz, int depth) {
    }
}
