package com.icthh.xm.lep.core;

import com.icthh.xm.lep.api.ScopedContext;

import java.util.Objects;

@Deprecated(forRemoval = true)
public class DefaultScopedContext extends DefaultContext implements ScopedContext {

    private final String scope;

    public DefaultScopedContext(String scope) {
        this.scope = Objects.requireNonNull(scope, "scope name can't be null");
    }

    @Override
    public String getScope() {
        return scope;
    }

}
