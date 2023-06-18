package com.icthh.xm.lep.api;

public interface LepMethod {

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

}
