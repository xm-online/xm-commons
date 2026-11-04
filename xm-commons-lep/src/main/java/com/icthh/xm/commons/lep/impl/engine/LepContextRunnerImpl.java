package com.icthh.xm.commons.lep.impl.engine;

import com.icthh.xm.commons.config.client.config.LepContextRunner;
import com.icthh.xm.commons.lep.api.LepManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LepContextRunnerImpl implements LepContextRunner {

    private final LepManagementService lepManagementService;

    public LepContextRunnerImpl(LepManagementService lepManagementService) {
        this.lepManagementService = lepManagementService;
    }

    @Override
    public void runInContext(Runnable task) {
        log.info("LepContextRunnerImpl-runInContext");
        lepManagementService.runInLepContext(task);
    }

    @Override
    public boolean isReady() {
        return lepManagementService.isLepEnginesInited();
    }
}
