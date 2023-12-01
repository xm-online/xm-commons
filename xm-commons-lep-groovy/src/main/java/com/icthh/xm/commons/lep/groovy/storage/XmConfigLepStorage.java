package com.icthh.xm.commons.lep.groovy.storage;

import com.icthh.xm.commons.lep.api.XmLepConfigFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class XmConfigLepStorage implements LepStorage {
    private final Map<String, XmLepConfigFile> leps;

    public XmConfigLepStorage(List<XmLepConfigFile> leps) {
        Map<String, XmLepConfigFile> lepsMap = new HashMap<>();
        leps.forEach(lep -> lepsMap.put(lep.getPath(), lep));
        this.leps = lepsMap;
    }

    public XmConfigLepStorage(Map<String, XmLepConfigFile> leps) {
        this.leps = leps;
    }

    @Override
    public void forEach(Consumer<XmLepConfigFile> action) {
        leps.values().forEach(action);
    }

    @Override
    public XmLepConfigFile getByPath(String path) {
        return leps.get(path);
    }

    @Override
    public boolean isExists(String path) {
        return leps.containsKey(path);
    }
}
