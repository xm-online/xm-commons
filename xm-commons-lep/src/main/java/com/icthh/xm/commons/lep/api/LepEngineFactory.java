package com.icthh.xm.commons.lep.api;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public abstract class LepEngineFactory {

    private final String appName;

    public abstract LepEngine createLepEngine(String tenant, List<XmLepConfigFile> configInLepFolder);
}
