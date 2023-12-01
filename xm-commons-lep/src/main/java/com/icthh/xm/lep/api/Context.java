package com.icthh.xm.lep.api;

import java.util.Map;
import java.util.Set;

@Deprecated(forRemoval = true)
public interface Context {

    /**
     * Read only context value names.
     *
     * @return read only context value names set view
     */
    Set<String> getNames();

    boolean contains(String name);

    Object getValue(String name);

    <T> T getValue(String name, Class<T> castToType);

    void setValue(String name, Object value);

    /**
     * Read only context values view.
     *
     * @return read only context values map view
     */
    Map<String, Object> getValues();

}
