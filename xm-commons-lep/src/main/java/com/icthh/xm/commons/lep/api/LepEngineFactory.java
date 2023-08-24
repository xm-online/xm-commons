package com.icthh.xm.commons.lep.api;

import java.util.List;

public abstract class LepEngineFactory {
    public abstract LepEngine createLepEngine(String tenant, List<XmLepConfigFile> configInLepFolder);
}
