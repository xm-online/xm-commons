package com.icthh.xm.commons.lep.js;

import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepEngineFactory;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@Slf4j
public class JsLepEngineFactory extends LepEngineFactory {

    private final LoggingWrapper loggingWrapper;


    public JsLepEngineFactory(String appName,
                              LoggingWrapper loggingWrapper) {
        super(appName);
        this.loggingWrapper = loggingWrapper;
    }

    @Override
    public LepEngine createLepEngine(String tenant, List<XmLepConfigFile> lepFromConfig) {
        Map<String, XmLepConfigFile> leps = new HashMap<>();
        lepFromConfig.stream()
            .filter(it -> it.getPath().endsWith(".js"))
            .forEach(it -> leps.put(it.getPath(), it));
        return new JsLepEngine(
            tenant,
            getAppName(),
            leps,
            loggingWrapper
        );
    }
}
