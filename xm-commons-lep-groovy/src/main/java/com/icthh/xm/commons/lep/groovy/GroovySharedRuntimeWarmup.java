package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.api.BaseLepContext;
import groovy.lang.GString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.codehaus.groovy.runtime.GStringImpl;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
 * <p>The warmup runs on a background daemon thread started from
 * {@code GroovyLepEngineFactory#setBeanClassLoader}: the factory is a dependency of the whole lep
 * subsystem, so it is guaranteed to exist - and the warmup to be running - before any engine is created.
 * (Do NOT move the trigger to a standalone bean: nothing depends on such a bean, spring creates it at an
 * arbitrary point of a minutes-long startup, and the engine init - driven independently by config events -
 * then passes {@link #awaitCompletion()} before the warmup even started. Found the hard way.) Lep traffic
 * cannot observe a partially warmed runtime: the {@code GroovyLepEngine} script warmup calls
 * {@link #awaitCompletion()} before the "lep engines inited" latch opens.
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

    /**
     * Deliberately contains NO java.util.Map types: the lep engine init script (InitLepEngine.groovy)
     * expando-patches {@code AbstractMap.metaClass}, and groovy propagates such a patch to a subclass only
     * if the subclass metaclass initializes AFTER the patch. Pre-initializing map metaclasses here would
     * freeze them before the init script runs and silently disable {@code map.properties} in leps.
     */
    private static final List<Class<?>> CORE_RECEIVER_CLASSES = List.of(
        GString.class, GStringImpl.class, String.class, Boolean.class, Integer.class, Long.class,
        Double.class, BigDecimal.class, ArrayList.class, LinkedHashSet.class, List.class, Set.class,
        Object.class
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
        // captured so the thread releases the latch of the warmup it belongs to
        CountDownLatch completion = done;
        Thread thread = new Thread(() -> warmup(classLoader, lepContextClass, completion), "lep-shared-runtime-warmup");
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
        warmup(classLoader, lepContextClass, done);
    }

    private static void warmup(ClassLoader classLoader,
                               Supplier<Class<? extends BaseLepContext>> lepContextClass,
                               CountDownLatch completion) {
        try {
            StopWatch stopWatch = StopWatch.createStarted();
            sweepClassLoaderMiss(classLoader);
            long sweepTime = stopWatch.getTime(MILLISECONDS);
            Set<Class<?>> warmed = warmupReceiverMetaClasses(lepContextClass);
            log.info("Lep shared runtime warmup done: classloader miss sweep {} ms, {} receiver metaclasses, total {} ms",
                sweepTime, warmed.size(), stopWatch.getTime(MILLISECONDS));
        } finally {
            completion.countDown();
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
            // maps are excluded for the same reason as in CORE_RECEIVER_CLASSES: the engine init
            // script expando-patches AbstractMap, which must happen before their metaclasses initialize
            if (clazz == null || clazz.isPrimitive() || Map.class.isAssignableFrom(clazz) || !visited.add(clazz)) {
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
