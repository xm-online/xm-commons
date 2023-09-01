package com.icthh.xm.commons.lep.spring.lepservice;

import com.icthh.xm.commons.lep.api.LepEngine;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClearServicesOnEngineDestroy implements LepEngine.DestroyCallback {

    private final LepServiceFactoryWithLepFactoryMethod factory;

    @Override
    public void onDestroy(LepEngine lepEngine) {
        factory.clear(lepEngine.getId());
    }
}
