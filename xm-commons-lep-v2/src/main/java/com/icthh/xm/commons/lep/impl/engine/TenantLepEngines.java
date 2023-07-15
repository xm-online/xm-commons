package com.icthh.xm.commons.lep.impl.engine;

import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepEngineSession;
import com.icthh.xm.commons.lep.api.LepKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.icthh.xm.commons.lep.impl.engine.TenantLepEngines.TenantLepEnginesStates.ACTIVE;
import static com.icthh.xm.commons.lep.impl.engine.TenantLepEngines.TenantLepEnginesStates.DESTROYED;
import static com.icthh.xm.commons.lep.impl.engine.TenantLepEngines.TenantLepEnginesStates.DESTROYING;

@Slf4j
@RequiredArgsConstructor
class TenantLepEngines {

    enum TenantLepEnginesStates {
        ACTIVE, DESTROYING, DESTROYED;
    }

    private final AtomicInteger countOfExecutions = new AtomicInteger();
    private final AtomicReference<TenantLepEnginesStates> state = new AtomicReference<>(ACTIVE);

    private final String tenant;
    private final List<LepEngine> lepEngines;

    public LepEngineSession openLepEngineSession(LepKey lepKey) {
        for (LepEngine lepEngine: lepEngines) {
            if (lepEngine.isExists(lepKey)) {
                // Don't change order. First - increment count of sessions, second - check that release allowed.
                countOfExecutions.incrementAndGet(); // Disable destroy, if not destroyed
                if (state.get() == ACTIVE) { // Check is not destroyed before increment.
                    return new LepEngineSessionImpl(Optional.of(lepEngine), this::onClose);
                } else {
                    onClose();
                    return DisabledLepEngineSession.INSTANCE;
                }
            }
        }

        return new NoLepSession();
    }

    public TenantLepEnginesStates getState() {
        return state.get();
    }

    public void destroy() {
        // disallow release new sessions
        if (!state.compareAndSet(ACTIVE, DESTROYING)) {
            return; // when already destroy in progress on destroyed
        }

        log.info("START | destroying lep engines for tenant {}", tenant);
        // check how many session was released before disabling
        int executions = countOfExecutions.get();
        destroyTenantLepEngine(executions);
    }

    private void onClose() {
        int executions = countOfExecutions.decrementAndGet();
        destroyTenantLepEngine(executions);
    }

    private void destroyTenantLepEngine(int executions) {
        if (executions == 0 && getState() == DESTROYING) {
            if (state.compareAndSet(DESTROYING, DESTROYED)) {
                lepEngines.forEach(this::destroyEngine);
                log.info("STOP | destroying lep engines for tenant {}", tenant);
            }
        }
    }

    private void destroyEngine(LepEngine engine) {
        try {
            engine.destroy();
        } catch (Throwable e) {
            log.error("Error during destroy engine", e);
        }
    }
}
