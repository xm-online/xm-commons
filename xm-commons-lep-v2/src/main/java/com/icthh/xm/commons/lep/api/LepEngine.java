package com.icthh.xm.commons.lep.api;

import com.icthh.xm.commons.lep.ProceedingLep;

public abstract class LepEngine {
    public abstract void destroy();
    public int order() {
        return 0;
    }
    public abstract boolean isExists(LepKey lepKey);
    public abstract Object invoke(LepKey lepKey, ProceedingLep lepMethod);
}
