package com.icthh.xm.commons.lep.impl.engine;

import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepEngineFactory;
import com.icthh.xm.commons.lep.api.LepEngineSession;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.api.LepExecutor;
import com.icthh.xm.commons.lep.api.LepKey;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static java.util.Comparator.comparingInt;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toUnmodifiableList;

@Slf4j
@Component
public class LepManagementServiceImpl implements LepManagementService {

    private final AtomicBoolean isLepConfigInited = new AtomicBoolean(false);
    private final LepEnginesManager lepEnginesManager = new LepEnginesManager();
    private final ThreadLocal<TenantLepEngines> tenantLepEnginesThreadContext = new ThreadLocal<>();

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
    public void refreshEngines(List<String> tenants, Map<String, String> configInLepFolder) {
        log.info("Start lep engines refresh by configs.size {}, for tenants {}", configInLepFolder.size(), tenants);
        log.trace("Start lep engines refresh by configs {}", configInLepFolder);

        tenants.forEach(tenantKey -> {
            String tenant = tenantKey.toUpperCase();
            StopWatch timer = StopWatch.createStarted();

            log.info("START | Create lep engines for tenant {}", tenant);
            var engines = engineFactories.stream()
                .map(it -> it.createLepEngine(tenant, configInLepFolder))
                .sorted(comparingInt(LepEngine::order))
                .collect(toUnmodifiableList());
            lepEnginesManager.update(tenant, engines);
            log.info("STOP | Finish creating lep engines for tenant {}, {}ms", tenant, timer.getTime(MILLISECONDS));
        });

        isLepConfigInited.set(true);
    }

    @Override
    public LepExecutor getLepExecutor(LepKey lepKey) {
        assertLepConfigInited();
        assertLepEngineInited();
        TenantLepEngines tenantLepEngines = getCurrentTenantLepEngines();
        return tenantLepEngines.getLepExecutor(lepKey);
    }

    @Override
    public LepEngineSession beginThreadContext() {
        assertLepConfigInited();
        String tenant = getTenantKeyFromThreadContext();
        TenantLepEngines tenantLepEngine = lepEnginesManager.acquireTenantLeapEngine(tenant);
        tenantLepEnginesThreadContext.set(tenantLepEngine);
        return this::endThreadContext;
    }

    @Override
    public void endThreadContext() {
        TenantLepEngines tenantLepEngines = tenantLepEnginesThreadContext.get();
        tenantLepEngines.releaseUsage();
        tenantLepEnginesThreadContext.remove();
    }

    private TenantLepEngines getCurrentTenantLepEngines() {
        return tenantLepEnginesThreadContext.get();
    }

    private String getTenantKeyFromThreadContext() {
        return getRequiredTenantKeyValue(tenantContextHolder);
    }

    private void assertLepConfigInited() {
        if (!isLepConfigInited.get()) {
            throw new IllegalStateException("Lep engines not inited");
        }
    }

    private void assertLepEngineInited() {
        if (tenantLepEnginesThreadContext.get() == null) {
            throw new IllegalStateException("Lep thread context not inited." +
                " Use try(var session = LepManagementService.beginThreadContext()){<you code>}");
        }
    }

}
