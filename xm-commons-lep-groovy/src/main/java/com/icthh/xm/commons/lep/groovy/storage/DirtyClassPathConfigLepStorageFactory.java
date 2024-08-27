package com.icthh.xm.commons.lep.groovy.storage;

import com.icthh.xm.commons.lep.api.XmLepConfigFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DirtyClassPathConfigLepStorageFactory implements LepStorageFactory {

    private final XmConfigLepStorageFactory xmConfigLepStorageFactory;
    private final Map<String, DirtyClassPathConfigLepStorage> storageByTenant = new ConcurrentHashMap<>();

    public DirtyClassPathConfigLepStorageFactory(XmConfigLepStorageFactory xmConfigLepStorageFactory) {
        this.xmConfigLepStorageFactory = xmConfigLepStorageFactory;
    }

    public DirtyClassPathConfigLepStorage buildXmConfigLepStorage(String tenant, List<XmLepConfigFile> lepFromConfig) {
        Map<String, XmLepConfigFile> leps = new HashMap<>();
        DirtyClassPathConfigLepStorage storage = storageByTenant.computeIfAbsent(tenant, t -> new DirtyClassPathConfigLepStorage());
        xmConfigLepStorageFactory.buildXmConfigLepStorage(tenant, lepFromConfig).forEach(it -> leps.put(it.getPath(), it));
        storage.update(leps);
        return storage;
    }

}
