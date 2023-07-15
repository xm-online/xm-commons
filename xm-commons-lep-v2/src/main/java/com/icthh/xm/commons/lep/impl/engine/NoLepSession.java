package com.icthh.xm.commons.lep.impl.engine;

import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepEngineSession;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class NoLepSession extends LepEngineSessionImpl {
    public NoLepSession() {
        super(Optional.empty(), () -> {});
    }

    @Override
    public LepEngineSession ifLepPresent(Function<LepEngine, Object> executeLepMethod) {
        return this;
    }

    @Override
    public LepEngineSession ifLepNotExists(Supplier<Object> executeOriginalMethod) {
        return super.ifLepNotExists(executeOriginalMethod);
    }

    @Override
    public Object getMethodResult() {
        return super.getMethodResult();
    }

    @Override
    public boolean isSessionActive() {
        return true;
    }

    @Override
    public void close() {
        // do nothing
    }
}
