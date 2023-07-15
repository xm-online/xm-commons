package com.icthh.xm.commons.lep.api;

import java.util.List;
import java.util.Map;

public interface LepEngineService {
    boolean isLepEnginesInited();
    void refreshEngines(List<String> tenants, Map<String, String> configInLepFolder);
    LepEngineSession openLepEngineSession(LepKey lepKey);
}
