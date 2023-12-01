package com.icthh.xm.lep.api;

import com.icthh.xm.commons.lep.api.LepBaseKey;

public interface LepMethod {

    // This method brakes encapsulation. Pls remove usage of this method from LEP scripts.
    @Deprecated(forRemoval = true)
    Object getTarget();

    MethodSignature getMethodSignature();

    Object[] getMethodArgValues();

    default <T> T getParameter(String name, Class<T> type) {
        Integer parameterIndex = getMethodSignature().getParameterIndex(name);
        if (parameterIndex == null) {
            throw new IllegalStateException("Can't find parameter '" + name + "' for method: "
                + getMethodSignature().toString());
        }
        return type.cast(getMethodArgValues()[parameterIndex]);
    }

    LepBaseKey getLepBaseKey();

}
