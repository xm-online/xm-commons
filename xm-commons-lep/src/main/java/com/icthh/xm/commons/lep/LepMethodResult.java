package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.LepInvocationCauseException;

import java.util.Optional;

/**
 * The {@link LepMethodResult} class.
 */
public class LepMethodResult {

    private final Object returnedValue;

    private final LepInvocationCauseException lepInvocationCauseException;

    private LepMethodResult(Object returnedValue, LepInvocationCauseException lepInvocationCauseException) {
        if (returnedValue != null && lepInvocationCauseException != null) {
            throw new IllegalArgumentException("returned value and lep exception can't both be not null");
        }
        this.returnedValue = returnedValue;
        this.lepInvocationCauseException = lepInvocationCauseException;
    }

    public static LepMethodResult valueOf(Object methodResult) {
        return new LepMethodResult(methodResult, null);
    }

    public static LepMethodResult valueOf(LepInvocationCauseException methodException) {
        return new LepMethodResult(null, methodException);
    }

    public static LepMethodResult valueOf(MethodResultProcessor methodResultProcessor) {
        if (methodResultProcessor.isError()) {
            return valueOf(methodResultProcessor.getLepInvocationCauseException());
        } else {
            return valueOf(methodResultProcessor.getReturnedValue());
        }
    }

    public Optional<Object> getReturnedValue() {
        return Optional.ofNullable(returnedValue);
    }

    public boolean isError() {
        return lepInvocationCauseException != null;
    }

}
