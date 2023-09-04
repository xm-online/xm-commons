package com.icthh.xm.commons.lep.impl.internal;

import com.icthh.xm.commons.lep.api.LepManagementService;
import lombok.extern.slf4j.Slf4j;

/**
 * Don't use it in lep!
 * This class exists only for temporary support CoreContextsHolder!
 * This class will be removed after migration from CoreContextsHolder;
 */
@Slf4j
@Deprecated(forRemoval = true)
public class MigrationFromCoreContextsHolderLepManagementServiceReference {

    private static volatile LepManagementService LEP_MANAGEMENT_SERVICE;

    public MigrationFromCoreContextsHolderLepManagementServiceReference(LepManagementService lepManagementService) {
        LEP_MANAGEMENT_SERVICE = lepManagementService;
    }

    public static LepManagementService getLepManagementServiceInstance() {
        log.error("You are using deprecated class LepManager or CoreContextsHolder. This classes will be removed. Migrate to LepManagementService or LepThreadHelper.");
        return LEP_MANAGEMENT_SERVICE;
    }

}
