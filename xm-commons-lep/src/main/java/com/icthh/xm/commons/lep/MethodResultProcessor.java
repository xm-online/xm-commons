package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.LepInvocationCauseException;

import javax.annotation.Nullable;

class MethodResultProcessor {

    @Nullable
    private final Object returnedValue;

    @Nullable
    private final LepInvocationCauseException methodException;

    MethodResultProcessor(@Nullable Object returnedValue, @Nullable LepInvocationCauseException methodException) {
        this.methodException = methodException;
        this.returnedValue = returnedValue;
    }

    static MethodResultProcessor valueOf(@Nullable Object methodResult) {
        return new MethodResultProcessor(methodResult, null);
    }

    static MethodResultProcessor valueOf(LepInvocationCauseException methodException) {
        return new MethodResultProcessor(null, methodException);
    }

    Object processResult() throws LepInvocationCauseException {
        if (getLepInvocationCauseException() != null) {
            throw getLepInvocationCauseException();
        }
        return getReturnedValue();
    }

    boolean isError() {
        return getLepInvocationCauseException() != null;
    }

    LepInvocationCauseException getLepInvocationCauseException() {
        return methodException;
    }

    Object getReturnedValue() {
        return returnedValue;
    }

}
