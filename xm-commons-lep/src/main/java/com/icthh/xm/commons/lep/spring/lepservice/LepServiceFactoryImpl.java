package com.icthh.xm.commons.lep.spring.lepservice;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LepServiceFactoryImpl implements LepServiceFactory {

    private final String scopeId;
    private final LepServiceFactoryWithLepFactoryMethod lepServiceFactory;

    @Override
    public <T> T getInstance(Class<T> lepServiceClass) {
        return lepServiceFactory.getInstance(scopeId, lepServiceClass);
    }

}
