package com.icthh.xm.commons.lep;


import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.MethodSignature;

import java.util.Objects;

/**
 * The {@link BaseProceedingLep} class.
 */
public abstract class BaseProceedingLep implements ProceedingLep {

    private final LepMethod lepMethod;

    public BaseProceedingLep(LepMethod lepMethod) {
        this.lepMethod = Objects.requireNonNull(lepMethod, "lepMethod can't be null");
    }

    @Override
    public Object getTarget() {
        return lepMethod.getTarget();
    }

    @Override
    public MethodSignature getMethodSignature() {
        return lepMethod.getMethodSignature();
    }

    @Override
    public Object[] getMethodArgValues() {
        return lepMethod.getMethodArgValues();
    }
}
