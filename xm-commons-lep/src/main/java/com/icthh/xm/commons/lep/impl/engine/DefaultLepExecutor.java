package com.icthh.xm.commons.lep.impl.engine;

import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepExecutor;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class DefaultLepExecutor implements LepExecutor {

    private final LepEngine lepEngine;
    private Object methodResult;

    @Override
    public LepExecutor ifLepPresent(Function<LepEngine, Object> executeLepMethod) {
        methodResult = executeLepMethod.apply(lepEngine);
        return this;
    }

    @Override
    public LepExecutor ifLepNotExists(Supplier<Object> executeOriginalMethod) {
        return this;
    }

    @Override
    public Object getMethodResult() {
        return methodResult;
    }

}
