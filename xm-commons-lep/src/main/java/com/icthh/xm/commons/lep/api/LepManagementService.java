package com.icthh.xm.commons.lep.api;

import java.util.List;
import java.util.Map;

public interface LepManagementService {
    boolean isLepEnginesInited();
    void refreshEngines(Map<String, List<XmLepConfigFile>> configInLepFolder);
    LepExecutor getLepExecutor(LepKey lepKey);

    LepEngineSession beginThreadContext();
    LepEngineSession beginThreadContext(LepExecutorResolver tenantLepEngines);
    LepExecutorResolver getCurrentLepExecutorResolver();
    void runInLepContext(Runnable task);

    /**
     * Same as LepEngineSession.close.
     * Use LepEngineSession.close from beginThreadContext instead, if possible.
     */
    void endThreadContext();


}
