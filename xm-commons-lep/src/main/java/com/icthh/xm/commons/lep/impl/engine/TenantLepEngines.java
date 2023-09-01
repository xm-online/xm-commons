package com.icthh.xm.commons.lep.impl.engine;

import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepExecutor;
import com.icthh.xm.commons.lep.api.LepExecutorResolver;
import com.icthh.xm.commons.lep.api.LepKey;
import com.icthh.xm.commons.logging.util.MdcUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.icthh.xm.commons.lep.impl.engine.TenantLepEngines.TenantLepEnginesStates.ACTIVE;
import static com.icthh.xm.commons.lep.impl.engine.TenantLepEngines.TenantLepEnginesStates.DESTROYED;
import static com.icthh.xm.commons.lep.impl.engine.TenantLepEngines.TenantLepEnginesStates.DESTROYING;

@Slf4j
@RequiredArgsConstructor
class TenantLepEngines implements LepExecutorResolver {

    enum TenantLepEnginesStates {
        ACTIVE, DESTROYING, DESTROYED;
    }

    private final AtomicInteger countOfExecutions = new AtomicInteger();
    private final AtomicReference<TenantLepEnginesStates> state = new AtomicReference<>(ACTIVE);
    private final String logId = MdcUtils.generateRid();

    private final String tenant;
    private final List<LepEngine> lepEngines;

    @Override
    public LepExecutor getLepExecutor(LepKey lepKey) {
        for (LepEngine lepEngine: lepEngines) {
            if (lepEngine.isExists(lepKey)) {
                return new DefaultLepExecutor(lepEngine);
            }
        }

        return new OriginalMethodLepExecutor();
    }

    @Override
    public void acquireUsage() {
        this.countOfExecutions.incrementAndGet();
    }

    @Override
    public void releaseUsage() {
        int executions = this.countOfExecutions.decrementAndGet();
        destroyTenantLepEngine(executions);
    }

    public boolean isActive() {
        return state.get() == ACTIVE;
    }

    public void destroy() {
        if (!state.compareAndSet(ACTIVE, DESTROYING)) {
            return; // when already destroy in progress or destroyed
        }

        log.debug("START | destroying lep engines for tenant {}", tenant);
    }

    private void destroyTenantLepEngine(int executions) {
        if (executions == 0 && state.get() == DESTROYING) {
            if (state.compareAndSet(DESTROYING, DESTROYED)) {
                lepEngines.forEach(this::destroyEngine);
                log.debug("STOP | destroying lep engines for tenant {}", tenant);
            }
        }
    }

    private void destroyEngine(LepEngine engine) {
        try {
            engine.destroy();
        } catch (Throwable e) {
            log.error("Error during destroy engine {}", this);
        }
    }

    @Override
    public String toString() {
        String id = tenant + '_' + logId;
        return "TenantLepEngines(" +
            "id=" + id +
            "state=" + state +
            "countOfExecution=" + countOfExecutions.get() +
            ")";
    }
}
