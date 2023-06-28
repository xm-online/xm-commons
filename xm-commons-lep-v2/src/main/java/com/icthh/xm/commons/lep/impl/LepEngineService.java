package com.icthh.xm.commons.lep.impl;

import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepEngineFactory;
import com.icthh.xm.commons.lep.api.LepKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toUnmodifiableList;

@Slf4j
@Component
public class LepEngineService {

    private final AtomicBoolean isLepEnginesInited = new AtomicBoolean(false);
    private final List<LepEngineFactory> engineFactories;
    private volatile List<LepEngine> engines = new ArrayList<>();

    public LepEngineService(List<LepEngineFactory> engineFactories) {
        this.engineFactories = engineFactories;
    }

    public boolean isLepEnginesInited() {
        return isLepEnginesInited.get();
    }

    public void refreshEngines(Map<String, String> configInLepFolder) {
        log.info("Start lep engines refresh by configs.size {}", configInLepFolder.size());
        log.trace("Start lep engines refresh by configs {}", configInLepFolder);

        var oldEngines = this.engines;
        this.engines = engineFactories.stream()
            .map(it -> it.createLepEngine(configInLepFolder))
            .sorted(comparingInt(LepEngine::order))
            .collect(toUnmodifiableList());
        isLepEnginesInited.set(true);
        log.info("Lep engines inited");

        oldEngines.forEach(LepEngine::destroy);
    }

    public Optional<LepEngine> findLepEngineByLepKey(LepKey lepKey) {
        assertLepEnginesInited();
        return this.engines.stream().filter(it -> it.isExists(lepKey)).findFirst();
    }

    private void assertLepEnginesInited() {
        if (!isLepEnginesInited.get()) {
            throw new IllegalStateException("Lep engines not inited");
        }
    }

}
