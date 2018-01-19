package com.icthh.xm.commons.request.internal;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@link XmRequestContextData} class.
 */
class XmRequestContextData {

    private static final ThreadLocal<XmRequestContextData> THREAD_LOCAL =
        ThreadLocal.withInitial(XmRequestContextData::new);

    static XmRequestContextData get() {
        return THREAD_LOCAL.get();
    }

    void destroyCurrent() {
        THREAD_LOCAL.remove();
    }

    private Map<String, Object> model = new HashMap<>();

    boolean containsKey(String key) {
        return model.containsKey(key);
    }

    <T> T getValue(String key, Class<T> type) {
        return type.cast(model.get(key));
    }

    <T> T getValueOrDefault(String key, Class<T> type, T defaultValue) {
        return type.cast(model.getOrDefault(key, defaultValue));
    }

    void putValue(String key, Object value) {
        model.put(key, value);
    }

}
