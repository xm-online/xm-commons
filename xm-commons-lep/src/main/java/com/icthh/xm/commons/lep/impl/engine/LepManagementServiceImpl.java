package com.icthh.xm.commons.lep.impl.engine;

import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepEngineFactory;
import com.icthh.xm.commons.lep.api.LepEngineSession;
import com.icthh.xm.commons.lep.api.LepExecutor;
import com.icthh.xm.commons.lep.api.LepExecutorResolver;
import com.icthh.xm.commons.lep.api.LepKey;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toUnmodifiableList;

@Slf4j
public class LepManagementServiceImpl implements LepManagementService {

    private final AtomicBoolean isLepConfigInited = new AtomicBoolean(false);
    private final LepEngineManager lepEnginesManager = new LepEngineManager();
    private final ThreadLocal<LepExecutorResolver> tenantLepEnginesThreadContext = new ThreadLocal<>();
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    private final List<LepEngineFactory> engineFactories;
    private final TenantContextHolder tenantContextHolder;
    private final List<LepEngine.DestroyCallback> destroyCallbacks;

    public LepManagementServiceImpl(List<LepEngineFactory> engineFactories,
                                    TenantContextHolder tenantContextHolder,
                                    List<LepEngine.DestroyCallback> destroyCallbacks) {
        this.engineFactories = engineFactories;
        this.tenantContextHolder = tenantContextHolder;
        this.destroyCallbacks = destroyCallbacks;
    }

    @Override
    public boolean isLepEnginesInited() {
        return isLepConfigInited.get();
    }

    @Override
    public void refreshEngines(Map<String, List<XmLepConfigFile>> configInLepFolder) {
        log.info("START | Start lep engines refresh for tenants {}", configInLepFolder.keySet());
        log.trace("Start lep engines refresh by configs {}", configInLepFolder.values());

        configInLepFolder.keySet().forEach(tenantKey -> {
            StopWatch timer = StopWatch.createStarted();

            String tenant = tenantKey.toUpperCase();
            List<XmLepConfigFile> tenantConfigs = configInLepFolder.getOrDefault(tenantKey, emptyList());
            log.info("START | Create lep engines for tenant: {} | configInLepFolder.size: {}", tenant, tenantConfigs.size());
            log.trace("START | Create lep engines for tenant: {} | configInLepFolder.size: {}", tenant, tenantConfigs);

            List<LepEngine> engines = createEngines(tenantKey, tenantConfigs);
            engines.forEach(engine -> destroyCallbacks.forEach(engine::addDestroyCallback));
            lepEnginesManager.update(tenant, engines);

            log.info("STOP | Finish creating lep engines for tenant {}, {}ms", tenant, timer.getTime(MILLISECONDS));
        });

        if (!isLepConfigInited.get() && isLepConfigInited.compareAndSet(false, true)) {
            countDownLatch.countDown();
        }

        log.info("STOP | Finish lep engines refresh");
    }

    private List<LepEngine> createEngines(String tenantKey, List<XmLepConfigFile> tenantConfigs) {
        return engineFactories.stream()
            .map(it -> it.createLepEngine(tenantKey, tenantConfigs))
            .sorted(comparingInt(LepEngine::order))
            .collect(toUnmodifiableList());
    }

    @Override
    public LepExecutor getLepExecutor(LepKey lepKey) {
        assertLepConfigInited();
        assertLepEngineInited();
        LepExecutorResolver tenantLepEngines = getCurrentLepExecutorResolver();
        return tenantLepEngines.getLepExecutor(lepKey);
    }


    @Override
    public LepEngineSession beginThreadContext(LepExecutorResolver tenantLepEngines) {
        assertLepConfigInited();
        if (tenantLepEnginesThreadContext.get() != null) {
            throw new IllegalStateException("Lep thread context already inited");
        }
        setLepThreadContext(tenantLepEngines);
        tenantLepEngines.acquireUsage();
        return this::endThreadContext;
    }

    @Override
    public void runInLepContext(Runnable task) {
        try(var threadContext = this.beginThreadContext()) {
            task.run();
        }
    }

    @Override
    public LepEngineSession beginThreadContext() {
        assertLepConfigInited();
        log.debug("Init thread lep context");
        LepExecutorResolver tenantLepEngines = tenantLepEnginesThreadContext.get();
        if (tenantLepEngines == null) {
            String tenant = getTenantKeyFromThreadContext();
            tenantLepEngines = lepEnginesManager.acquireTenantLepEngine(tenant, (tenantKey) -> createEngines(tenantKey, List.of()));
            setLepThreadContext(tenantLepEngines);
            return this::endThreadContext;
        } else {
            endThreadContext();
            LepEngineSession threadContext = beginThreadContext();
            log.warn("Lep thread context recreated");
            return threadContext;
        }
    }

    @Override
    public void endThreadContext() {
        LepExecutorResolver tenantLepEngines = tenantLepEnginesThreadContext.get();
        if (tenantLepEngines != null) {
            tenantLepEngines.releaseUsage();
        }
        removeLepThreadContext();
    }


    private void setLepThreadContext(LepExecutorResolver tenantLepEngines) {
        if (log.isDebugEnabled()) {
            log.debug("Lep context inited with tenantLepEngines: {} | thread: {}", tenantLepEngines, Thread.currentThread().getId());
        }
        tenantLepEnginesThreadContext.set(tenantLepEngines);
    }

    private void removeLepThreadContext() {
        tenantLepEnginesThreadContext.remove();
        if (log.isDebugEnabled()) {
            log.debug("End thread lep context | thread: {}", Thread.currentThread().getId());
        }
    }

    @Override
    public LepExecutorResolver getCurrentLepExecutorResolver() {
        return tenantLepEnginesThreadContext.get();
    }

    private String getTenantKeyFromThreadContext() {
        return getRequiredTenantKeyValue(tenantContextHolder).toUpperCase();
    }

    @SneakyThrows
    private void assertLepConfigInited() {
        if (!isLepConfigInited.get()) {
            log.warn("Lep engines not inited");
            countDownLatch.await();
        }
    }

    private void assertLepEngineInited() {
        if (tenantLepEnginesThreadContext.get() == null) {
            log.error("Lep thread context not inited | thread: {}", Thread.currentThread().getId());
            throw new IllegalStateException("Lep thread context not inited." +
                " Use try(var session = LepManagementService.beginThreadContext()){<you code>}");
        }
    }

}
