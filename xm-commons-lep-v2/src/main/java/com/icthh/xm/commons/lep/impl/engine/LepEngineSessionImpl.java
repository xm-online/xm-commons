package com.icthh.xm.commons.lep.impl.engine;

import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepEngineSession;
import lombok.RequiredArgsConstructor;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class LepEngineSessionImpl implements LepEngineSession {

    private final Optional<LepEngine> lepEngine;
    private final Closeable onFinishAction;

    private Object methodResult;
    private boolean isActive = true;

    @Override
    public void close() throws IOException {
        isActive = false;
        onFinishAction.close();
    }

    @Override
    public LepEngineSession ifLepPresent(Function<LepEngine, Object> executeLepMethod) {
        lepEngine.ifPresent(engine -> methodResult = executeLepMethod.apply(engine));
        return this;
    }

    @Override
    public LepEngineSession ifLepNotExists(Supplier<Object> executeOriginalMethod) {
        if (lepEngine.isEmpty()) {
            methodResult = executeOriginalMethod.get();
        }
        return this;
    }

    @Override
    public Object getMethodResult() {
        return methodResult;
    }

    @Override
    public boolean isSessionActive() {
        return isActive;
    }
}
