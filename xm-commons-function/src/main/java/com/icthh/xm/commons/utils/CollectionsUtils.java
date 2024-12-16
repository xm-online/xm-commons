package com.icthh.xm.commons.utils;

import com.google.common.collect.Maps;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.Map;

@UtilityClass
public class CollectionsUtils {

    /**
     * Return immutable empty map, if map is empty
     * @param map input map
     * @param <K> key
     * @param <V> value
     * @return original map or EmptyMap
     */
    public static <K, V> Map<K, V> nullSafe(Map<K, V> map) {
        return map == null ? Collections.emptyMap() : map;
    }

    /**
     * Return new mutable HashMap instance if input is null
     * @param map input map
     * @param <K> key
     * @param <V> value
     * @return original map or new HashMap (mutable)
     */
    public static <K, V> Map<K, V> getOrEmpty(Map<K, V> map) {
        return map == null ? Maps.newHashMap() : map;
    }
}
