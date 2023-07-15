package com.icthh.xm.commons.lep.impl.engine;

import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepEngineSession;
import com.icthh.xm.commons.lep.api.LepKey;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class LepEnginesManager {

    private final Map<String, TenantLepEngines> enginesByTenants = new ConcurrentHashMap<>();

    public LepEngineSession openLepEngineSession(String tenant, LepKey lepKey) {
        LepEngineSession lepEngineSession = openLepEngineSessionForTenant(tenant, lepKey);
        int retryCount = 0;
        // can happen during refresh
        while (!lepEngineSession.isSessionActive()) {
            retryCount++;
            if (retryCount >= 3) {
                throw new IllegalStateException("Lep engine already closed");
            }
            lepEngineSession = openLepEngineSessionForTenant(tenant, lepKey);
        }

        return lepEngineSession;
    }

    private LepEngineSession openLepEngineSessionForTenant(String tenant, LepKey lepKey) {
        TenantLepEngines tenantLepEngines = enginesByTenants.get(tenant);
        if (tenantLepEngines == null) {
            return new NoLepSession();
        }
        return tenantLepEngines.openLepEngineSession(lepKey);
    }

    public void update(String tenant, List<LepEngine> engines) {
        TenantLepEngines oldTenantLepEngines = enginesByTenants.get(tenant);
        enginesByTenants.put(tenant, new TenantLepEngines(tenant, engines));
        if (oldTenantLepEngines != null) {
            oldTenantLepEngines.destroy();
        }
    }
}
