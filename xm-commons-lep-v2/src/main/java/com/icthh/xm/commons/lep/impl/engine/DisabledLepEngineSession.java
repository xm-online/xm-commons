package com.icthh.xm.commons.lep.impl.engine;

import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepEngineSession;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

public class DisabledLepEngineSession implements LepEngineSession {

    public static final DisabledLepEngineSession INSTANCE = new DisabledLepEngineSession();

    @Override
    public LepEngineSession ifLepPresent(Function<LepEngine, Object> executeLepMethod) {
        throw new UnsupportedOperationException("Disabled session can't run lep methods");
    }

    @Override
    public LepEngineSession ifLepNotExists(Supplier<Object> executeOriginalMethod) {
        throw new UnsupportedOperationException("Disabled session can't run methods");
    }

    @Override
    public Object getMethodResult() {
        throw new UnsupportedOperationException("Disabled session can't return result");
    }

    @Override
    public boolean isSessionActive() {
        return false;
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException("Disabled session can't be closed");
    }
}
