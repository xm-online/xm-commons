package com.icthh.xm.commons.request.internal;

import com.icthh.xm.commons.request.XmPrivilegedRequestContext;

import java.util.Objects;

/**
 * The {@link DataAdapterXmPrivilegedRequestContext} class.
 */
class DataAdapterXmPrivilegedRequestContext implements XmPrivilegedRequestContext {

    private final XmRequestContextData data;

    DataAdapterXmPrivilegedRequestContext(XmRequestContextData data) {
        this.data = Objects.requireNonNull(data, "data can't be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putValue(String key, Object value) {
        data.putValue(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroyCurrentContext() {
        data.destroyCurrent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getValue(String key, Class<T> type) {
        return data.getValue(key, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getValueOrDefault(String key, Class<T> type, T defaultValue) {
        return data.getValueOrDefault(key, type, defaultValue);
    }

}
