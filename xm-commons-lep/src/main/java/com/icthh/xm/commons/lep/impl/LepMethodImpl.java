package com.icthh.xm.commons.lep.impl;

import com.icthh.xm.commons.lep.api.LepBaseKey;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.MethodSignature;
import lombok.Getter;

import java.util.Objects;

public class LepMethodImpl implements LepMethod {

    private static final Object[] EMPTY_OBJ_ARRAY = new Object[0];

    @Getter
    private final Object target;
    private final MethodSignature methodSignature;
    private final Object[] methodArgValues;
    private final LepBaseKey lepBaseKey;

    public LepMethodImpl(Object target,
                         MethodSignature methodSignature,
                         Object[] methodArgValues,
                         LepBaseKey lepBaseKey) {
        this.target = target;
        this.methodSignature = Objects.requireNonNull(methodSignature, "methodSignature can't be null");
        this.methodArgValues = methodArgValues == null ? EMPTY_OBJ_ARRAY : methodArgValues;
        this.lepBaseKey = lepBaseKey;
    }

    @Override
    public MethodSignature getMethodSignature() {
        return methodSignature;
    }

    @Override
    public Object[] getMethodArgValues() {
        return methodArgValues;
    }

    @Override
    public LepBaseKey getLepBaseKey() {
        return lepBaseKey;
    }

}
