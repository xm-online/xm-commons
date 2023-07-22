package com.icthh.xm.commons.lep.api;

import java.util.function.Function;
import java.util.function.Supplier;

public interface LepExecutor {
    LepExecutor ifLepPresent(Function<LepEngine, Object> executeLepMethod);
    LepExecutor ifLepNotExists(Supplier<Object> executeOriginalMethod);
    Object getMethodResult();
}
