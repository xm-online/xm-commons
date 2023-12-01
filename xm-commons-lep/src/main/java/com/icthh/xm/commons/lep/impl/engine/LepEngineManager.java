package com.icthh.xm.commons.lep.impl.engine;

import com.icthh.xm.commons.lep.api.LepEngine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

class LepEngineManager {

    private final Map<String, TenantLepEngine> enginesByTenants = new ConcurrentHashMap<>();

    public void update(String tenant, List<LepEngine> engines) {
        TenantLepEngine oldTenantLepEngines = enginesByTenants.get(tenant);
        enginesByTenants.put(tenant, new TenantLepEngine(tenant, engines));
        if (oldTenantLepEngines != null) {
            oldTenantLepEngines.acquireUsage();
            oldTenantLepEngines.destroy();
            oldTenantLepEngines.releaseUsage();
        }
    }

    public TenantLepEngine acquireTenantLepEngine(String tenant, Function<String, List<LepEngine>> engines) {
        TenantLepEngine tenantLepEngines = enginesByTenants.get(tenant);
        if (tenantLepEngines == null) {
            tenantLepEngines = enginesByTenants.computeIfAbsent(tenant, (key) -> new TenantLepEngine(tenant, engines.apply(tenant)));
        }

        int retryCount = 0;
        tenantLepEngines.acquireUsage();
        while (!tenantLepEngines.isActive()) {
            tenantLepEngines.releaseUsage();
            retryCount++;
            if (retryCount >= 3) {
                throw new IllegalStateException("Lep engine already closed");
            }
            tenantLepEngines = enginesByTenants.get(tenant);
        }

        return tenantLepEngines;
    }
}
