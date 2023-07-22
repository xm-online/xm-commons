package com.icthh.xm.lep.api;

@Deprecated(forRemoval = true)
public interface ContextsHolder {

    /**
     * Return context instance by scope name.
     *
     * @param scope context scope name, this name is unique across all
     *              scopes in one {@link ContextsHolder} instance
     * @return context instance
     * @see ContextScopes
     */
    ScopedContext getContext(String scope);

}
