package com.icthh.xm.commons.metric.config;

import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepContextFactory;
import com.icthh.xm.lep.api.LepMethod;

public class LepContextFactoryImpl implements LepContextFactory {
    @Override
    public BaseLepContext buildLepContext(LepMethod lepMethod) {
        return new LepContext();
    }
}
