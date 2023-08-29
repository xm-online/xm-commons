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
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static java.util.Comparator.comparingInt;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toUnmodifiableList;

@Slf4j
public class LepManagementServiceImpl implements LepManagementService {

    private final AtomicBoolean isLepConfigInited = new AtomicBoolean(false);
    private final LepEnginesManager lepEnginesManager = new LepEnginesManager();
    private final ThreadLocal<LepExecutorResolver> tenantLepEnginesThreadContext = new ThreadLocal<>();
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    private final List<LepEngineFactory> engineFactories;
    private final TenantContextHolder tenantContextHolder;

    public LepManagementServiceImpl(List<LepEngineFactory> engineFactories, TenantContextHolder tenantContextHolder) {
        this.engineFactories = engineFactories;
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    public boolean isLepEnginesInited() {
        return isLepConfigInited.get();
    }

    @Override
    public void refreshEngines(Map<String, List<XmLepConfigFile>> configInLepFolder) {
        log.info("Start lep engines refresh for tenants {}", configInLepFolder.keySet());
        log.trace("Start lep engines refresh by configs {}", configInLepFolder.values());

        configInLepFolder.keySet().forEach(tenantKey -> {
            String tenant = tenantKey.toUpperCase();
            StopWatch timer = StopWatch.createStarted();

            log.info("START | Create lep engines for tenant {}", tenant);
            var engines = engineFactories.stream()
                .map(it -> it.createLepEngine(tenant, configInLepFolder.get(tenantKey)))
                .sorted(comparingInt(LepEngine::order))
                .collect(toUnmodifiableList());
            lepEnginesManager.update(tenant, engines);
            log.info("STOP | Finish creating lep engines for tenant {}, {}ms", tenant, timer.getTime(MILLISECONDS));
        });

        if (!isLepConfigInited.get() && isLepConfigInited.compareAndSet(false, true)) {
            countDownLatch.countDown();
        }

        // if refresh operation invoked in thread where inited threadLepContext, threadLepContext have to be reinited
        LepExecutorResolver currentLepExecutorResolver = getCurrentLepExecutorResolver();
        if (currentLepExecutorResolver != null) {
            endThreadContext();
            beginThreadContext();
        }
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
        tenantLepEnginesThreadContext.set(tenantLepEngines);
        return beginThreadContext();
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
        LepExecutorResolver tenantLepEngines = tenantLepEnginesThreadContext.get();
        if (tenantLepEngines == null) {
            String tenant = getTenantKeyFromThreadContext();
            tenantLepEngines = lepEnginesManager.acquireTenantLepEngine(tenant);
            tenantLepEnginesThreadContext.set(tenantLepEngines);
        } else {
            tenantLepEngines.acquireUsage();
        }
        return this::endThreadContext;
    }

    @Override
    public void endThreadContext() {
        LepExecutorResolver tenantLepEngines = tenantLepEnginesThreadContext.get();
        if (tenantLepEngines != null) {
            tenantLepEngines.releaseUsage();
        }
        tenantLepEnginesThreadContext.remove();
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
            throw new IllegalStateException("Lep thread context not inited." +
                " Use try(var session = LepManagementService.beginThreadContext()){<you code>}");
        }
    }

}
