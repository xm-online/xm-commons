package com.icthh.xm.commons.lep.impl.engine;

import com.icthh.xm.commons.lep.ProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepEngineFactory;
import com.icthh.xm.commons.lep.api.LepKey;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LepEnginesParallelRefreshUnitTest {

    private static final int TENANTS = 8;

    @Test
    public void refreshCreatesEnginesForEveryTenant() {
        RecordingFactory factory = new RecordingFactory();
        LepManagementServiceImpl service = new LepManagementServiceImpl(List.of(factory), null, emptyList());

        service.refreshEngines(tenants());

        assertEquals(tenants().keySet(), factory.tenants.keySet());
        assertEquals(TENANTS, factory.tenants.size());
        assertTrue("lep engines expected to be marked as inited", service.isLepEnginesInited());
    }

    @Test
    public void refreshCreatesEnginesOfDifferentTenantsInParallel() {
        int expectedParallelism = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
        if (expectedParallelism < 2) {
            return; // single core agent: the refresh stays sequential by design
        }

        RecordingFactory factory = new RecordingFactory();
        LepManagementServiceImpl service = new LepManagementServiceImpl(List.of(factory), null, emptyList());

        service.refreshEngines(tenants());

        Set<String> threads = Set.copyOf(factory.tenants.values());
        assertTrue("expected several threads to build engines, but all ran on " + threads,
            threads.size() > 1);
        assertTrue("parallelism must stay within half of the cores, was " + threads.size(),
            threads.size() <= expectedParallelism);
    }

    @Test
    public void refreshOfASingleTenantStaysOnTheCallingThread() {
        RecordingFactory factory = new RecordingFactory();
        LepManagementServiceImpl service = new LepManagementServiceImpl(List.of(factory), null, emptyList());

        service.refreshEngines(Map.of("TENANT0", emptyList()));

        assertEquals(Set.of(Thread.currentThread().getName()), Set.copyOf(factory.tenants.values()));
    }

    private static Map<String, List<XmLepConfigFile>> tenants() {
        return IntStream.range(0, TENANTS).boxed().collect(Collectors.toMap(
            it -> "TENANT" + it, _ -> emptyList(), (a, _) -> a, LinkedHashMap::new));
    }

    private static class RecordingFactory extends LepEngineFactory {

        private final Map<String, String> tenants = new ConcurrentHashMap<>();

        RecordingFactory() {
            super("testApp");
        }

        @Override
        public LepEngine createLepEngine(String tenant, List<XmLepConfigFile> configInLepFolder) {
            tenants.put(tenant, Thread.currentThread().getName());
            // engine creation is slow in reality - hold the thread so the tenants really overlap
            busyWait();
            return new NoopLepEngine();
        }

        @Override
        public LepEngine createLepEngine(String tenant, List<XmLepConfigFile> configInLepFolder, String targetDirectoryPath) {
            return createLepEngine(tenant, configInLepFolder);
        }

        private static void busyWait() {
            long until = System.nanoTime() + 50_000_000L;
            while (System.nanoTime() < until) {
                Thread.onSpinWait();
            }
        }
    }

    private static class NoopLepEngine extends LepEngine {

        @Override
        public boolean isExists(LepKey lepKey) {
            return false;
        }

        @Override
        public Object invoke(LepKey lepKey, ProceedingLep lepMethod, BaseLepContext lepContext) {
            return null;
        }
    }
}
