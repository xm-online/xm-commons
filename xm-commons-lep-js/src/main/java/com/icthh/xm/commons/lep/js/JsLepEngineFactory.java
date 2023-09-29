package com.icthh.xm.commons.lep.js;

import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepEngineFactory;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.impl.LoggingWrapper;
import com.icthh.xm.commons.lep.spring.ApplicationNameProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JsLepEngineFactory extends LepEngineFactory {

    private final LoggingWrapper loggingWrapper;


    public JsLepEngineFactory(ApplicationNameProvider applicationNameProvider,
                              LoggingWrapper loggingWrapper) {
        super(applicationNameProvider.getAppName());
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
