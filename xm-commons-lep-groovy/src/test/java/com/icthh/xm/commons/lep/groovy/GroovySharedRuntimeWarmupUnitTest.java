package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.api.BaseLepContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GroovySharedRuntimeWarmupUnitTest {

    @BeforeEach
    @AfterEach
    public void resetWarmupState() {
        GroovySharedRuntimeWarmup.resetForTests();
    }

    /** the classloader miss path is what a packed WAR resolves expensively - the probe must reach it */
    @Test
    public void sweepProbeIssuesOneMissLookupOnTheGivenClassLoader() {
        RecordingClassLoader classLoader = new RecordingClassLoader();

        GroovySharedRuntimeWarmup.warmup(classLoader, null);

        long probeLookups = classLoader.requestedNames.stream()
            .filter(GroovySharedRuntimeWarmup.MISS_PROBE_CLASS_NAME::equals)
            .count();
        assertTrue(probeLookups >= 1, "miss probe was not looked up on the passed classloader");
    }

    @Test
    public void warmupRunsOncePerJvm() {
        RecordingClassLoader classLoader = new RecordingClassLoader();

        assertTrue(GroovySharedRuntimeWarmup.warmupOnceAsync(classLoader, null));
        assertFalse(GroovySharedRuntimeWarmup.warmupOnceAsync(classLoader, null),
            "second call must not start the warmup again");

        awaitProbeLookup(classLoader);
    }

    @Test
    public void receiverGraphCoversLepContextClassAndItsFieldTypes() {
        Set<Class<?>> receivers = GroovySharedRuntimeWarmup.collectReceiverClasses(() -> TestLepContext.class);

        assertTrue(receivers.contains(TestLepContext.class), "lep context class itself");
        assertTrue(receivers.contains(TestServices.class), "lepContext.<field> receiver");
        assertTrue(receivers.contains(TestService.class), "lepContext.<field>.<field> receiver");
        assertTrue(receivers.contains(groovy.lang.GString.class), "core groovy receiver");
    }

    @Test
    public void receiverGraphIgnoresPrivateStaticAndPrimitiveFields() {
        Set<Class<?>> receivers = GroovySharedRuntimeWarmup.collectReceiverClasses(() -> TestLepContext.class);

        assertFalse(receivers.contains(TestHidden.class), "private field type must not be collected");
        assertFalse(receivers.contains(TestStaticOnly.class), "static field type must not be collected");
    }

    @Test
    public void awaitCompletionReturnsImmediatelyWhenWarmupNeverStarted() {
        long start = System.currentTimeMillis();
        GroovySharedRuntimeWarmup.awaitCompletion();
        assertTrue(System.currentTimeMillis() - start < 1000, "await must not block when warmup disabled");
    }

    @Test
    public void awaitCompletionBlocksUntilStartedWarmupFinishes() {
        RecordingClassLoader classLoader = new RecordingClassLoader();
        assertTrue(GroovySharedRuntimeWarmup.warmupOnceAsync(classLoader, () -> TestLepContext.class));

        GroovySharedRuntimeWarmup.awaitCompletion();

        // after await the probe lookup must already be visible - no polling needed
        assertTrue(classLoader.requestedNames.contains(GroovySharedRuntimeWarmup.MISS_PROBE_CLASS_NAME),
            "await returned before the warmup finished");
    }

    @Test
    public void nullAndFailingLepContextSuppliersAreTolerated() {
        GroovySharedRuntimeWarmup.warmup(new RecordingClassLoader(), null);
        GroovySharedRuntimeWarmup.resetForTests();
        GroovySharedRuntimeWarmup.warmup(new RecordingClassLoader(), () -> {
            throw new IllegalStateException("detector failed");
        });
    }

    /** the Starter bean is the spring entry point: receiving the bean classloader starts the warmup */
    @Test
    public void starterRunsWarmupWhenBeanClassLoaderIsSet() {
        RecordingClassLoader classLoader = new RecordingClassLoader();

        new GroovySharedRuntimeWarmup.Starter(() -> TestLepContext.class).setBeanClassLoader(classLoader);

        awaitProbeLookup(classLoader);
    }

    private void awaitProbeLookup(RecordingClassLoader classLoader) {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (classLoader.requestedNames.contains(GroovySharedRuntimeWarmup.MISS_PROBE_CLASS_NAME)) {
                return;
            }
            Thread.onSpinWait();
        }
        throw new AssertionError("async warmup never looked up the miss probe on the classloader");
    }

    private static class RecordingClassLoader extends ClassLoader {
        private final ConcurrentLinkedQueue<String> requestedNames = new ConcurrentLinkedQueue<>();

        RecordingClassLoader() {
            super(GroovySharedRuntimeWarmupUnitTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            requestedNames.add(name);
            return super.loadClass(name, resolve);
        }
    }

    public static class TestLepContext extends BaseLepContext {
        public TestServices services;
        public int primitiveField;
        private TestHidden hidden;
        public static TestStaticOnly staticField;
    }

    public static class TestServices {
        public TestService someService;
        public List<String> names;
        public Map<String, Object> data;
    }

    public static class TestService {
    }

    public static class TestHidden {
    }

    public static class TestStaticOnly {
    }
}
