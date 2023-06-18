package com.icthh.xm.commons.lep.impl;

import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.MethodSignature;

import java.util.Arrays;
import java.util.Objects;

public class LepMethodImpl implements LepMethod {

    private static final Object[] EMPTY_OBJ_ARRAY = new Object[0];

    private final Object target;
    private final MethodSignature methodSignature;
    private final Object[] methodArgValues;

    /**
     * LEP method constructor with method arguments.
     *
     * @param target          method target
     * @param methodSignature method signature
     * @param methodArgValues method argument values
     */
    public LepMethodImpl(Object target,
                         MethodSignature methodSignature,
                         Object[] methodArgValues) {
        this.target = target;
        this.methodSignature = Objects.requireNonNull(methodSignature, "methodSignature can't be null");
        this.methodArgValues = (methodArgValues == null) ? EMPTY_OBJ_ARRAY
            : Arrays.copyOf(methodArgValues, methodArgValues.length);
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public MethodSignature getMethodSignature() {
        return methodSignature;
    }

    @Override
    public Object[] getMethodArgValues() {
        return Arrays.copyOf(methodArgValues, methodArgValues.length);
    }

}
