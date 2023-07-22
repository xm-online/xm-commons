package com.icthh.xm.commons.lep.impl;

import lombok.Getter;

import java.lang.reflect.Method;

public final class MethodEqualsByReferenceWrapper {
    @Getter
    private final Method method;

    private MethodEqualsByReferenceWrapper(Method method) {
        this.method = method;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MethodEqualsByReferenceWrapper && method == ((MethodEqualsByReferenceWrapper)obj).method;
    }

    @Override
    public int hashCode() {
        return method.hashCode();
    }

    public static MethodEqualsByReferenceWrapper wrap(Method method) {
        return new MethodEqualsByReferenceWrapper(method);
    }
}
