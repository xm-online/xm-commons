package com.icthh.xm.commons.lep.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public abstract class LepEngineFactory {

    @Getter
    private final String appName;

    public abstract LepEngine createLepEngine(String tenant, List<XmLepConfigFile> configInLepFolder);
}
