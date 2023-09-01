package com.icthh.xm.commons.lep.groovy.storage;

import com.icthh.xm.commons.lep.api.XmLepConfigFile;

import java.util.List;

public interface LepStorageFactory {

    String GROOVY_SUFFIX = ".groovy";

    LepStorage buildXmConfigLepStorage(String tenant, List<XmLepConfigFile> lepStorage);
}
