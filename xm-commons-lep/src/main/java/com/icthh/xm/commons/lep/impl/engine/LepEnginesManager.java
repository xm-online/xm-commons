package com.icthh.xm.commons.lep.impl.engine;

import com.icthh.xm.commons.lep.api.LepEngine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class LepEnginesManager {

    private final Map<String, TenantLepEngines> enginesByTenants = new ConcurrentHashMap<>();

    public void update(String tenant, List<LepEngine> engines) {
        TenantLepEngines oldTenantLepEngines = enginesByTenants.get(tenant);
        enginesByTenants.put(tenant, new TenantLepEngines(tenant, engines));
        if (oldTenantLepEngines != null) {
            oldTenantLepEngines.acquireUsage();
            oldTenantLepEngines.destroy();
            oldTenantLepEngines.releaseUsage();
        }
    }

    public TenantLepEngines acquireTenantLeapEngine(String tenant) {
        TenantLepEngines tenantLepEngines = enginesByTenants.get(tenant);
        if (tenantLepEngines == null) {
            tenantLepEngines = enginesByTenants.computeIfAbsent(tenant, (key) -> new TenantLepEngines(tenant, List.of()));
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
