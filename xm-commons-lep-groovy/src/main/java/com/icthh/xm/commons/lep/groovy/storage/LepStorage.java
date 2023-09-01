package com.icthh.xm.commons.lep.groovy.storage;

import com.icthh.xm.commons.lep.api.XmLepConfigFile;

import java.util.function.Consumer;

public interface LepStorage {
    void forEach(Consumer<XmLepConfigFile> lep);
    XmLepConfigFile getByPath(String path);
    boolean isExists(String path);
}
