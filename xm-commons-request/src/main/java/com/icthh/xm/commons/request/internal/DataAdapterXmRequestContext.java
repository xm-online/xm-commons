package com.icthh.xm.commons.request.internal;

import com.icthh.xm.commons.request.XmRequestContext;

import java.util.Objects;

/**
 * The {@link DataAdapterXmRequestContext} class.
 */
class DataAdapterXmRequestContext implements XmRequestContext {

    private final XmRequestContextData data;

    DataAdapterXmRequestContext(XmRequestContextData data) {
        this.data = Objects.requireNonNull(data, "data can't be null");
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
