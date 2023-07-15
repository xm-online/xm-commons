package com.icthh.xm.commons.lep.api;

import java.io.Closeable;
import java.util.function.Function;
import java.util.function.Supplier;

public interface LepEngineSession extends Closeable {
    LepEngineSession ifLepPresent(Function<LepEngine, Object> executeLepMethod);
    LepEngineSession ifLepNotExists(Supplier<Object> executeOriginalMethod);
    Object getMethodResult();
    boolean isSessionActive();
}
