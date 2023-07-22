package com.icthh.xm.commons.lep.api;

import java.util.List;
import java.util.Map;

public interface LepManagementService {
    boolean isLepEnginesInited();
    void refreshEngines(List<String> tenants, Map<String, String> configInLepFolder);
    LepExecutor getLepExecutor(LepKey lepKey);

    LepEngineSession beginThreadContext();
    LepEngineSession beginThreadContext(LepExecutorResolver tenantLepEngines);
    LepExecutorResolver getCurrentLepExecutorResolver();

    /**
     * @deprecated it`s temporary method for migration period
     * pls use LepEngineSession.close instead
     */
    @Deprecated(forRemoval = true)
    void endThreadContext();


}
