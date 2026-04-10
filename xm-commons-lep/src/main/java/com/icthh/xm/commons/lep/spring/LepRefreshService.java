package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

public interface LepRefreshService {
    Future<?> refreshEngines(Set<String> tenantsToUpdate, Map<String, Map<String, XmLepConfigFile>> scriptsByTenant, boolean isInit);

    void initOrRefresh(Set<String> tenantsToUpdate, Map<String, Map<String, XmLepConfigFile>> scriptsByTenant, String pathToPrecompiledLep);
}
