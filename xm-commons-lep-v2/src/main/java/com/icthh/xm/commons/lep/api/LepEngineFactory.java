package com.icthh.xm.commons.lep.api;

import java.util.Map;

public abstract class LepEngineFactory {
    public abstract LepEngine createLepEngine(Map<String, String> configInLepFolder);
}
