package com.icthh.xm.commons.lep;


import com.icthh.xm.commons.lep.api.LepBaseKey;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.MethodSignature;

import java.util.Objects;

public abstract class BaseProceedingLep implements ProceedingLep {

    private final LepMethod lepMethod;

    public BaseProceedingLep(LepMethod lepMethod) {
        this.lepMethod = Objects.requireNonNull(lepMethod, "lepMethod can't be null");
    }

    @Override
    public MethodSignature getMethodSignature() {
        return lepMethod.getMethodSignature();
    }

    @Override
    public Object[] getMethodArgValues() {
        return lepMethod.getMethodArgValues();
    }

    @Override
    public LepBaseKey getLepBaseKey() {
        return lepMethod.getLepBaseKey();
    }
}
