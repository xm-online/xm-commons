package com.icthh.xm.commons.lep.impl.engine;

import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepExecutor;

import java.util.function.Function;
import java.util.function.Supplier;

public class OriginalMethodLepExecutor extends DefaultLepExecutor {
    public OriginalMethodLepExecutor() {
        super(null);
    }

    @Override
    public LepExecutor ifLepPresent(Function<LepEngine, Object> executeLepMethod) {
        return this;
    }

    @Override
    public LepExecutor ifLepNotExists(Supplier<Object> executeOriginalMethod) {
        setMethodResult(executeOriginalMethod.get());
        return this;
    }

    @Override
    public Object getMethodResult() {
        return super.getMethodResult();
    }
}
