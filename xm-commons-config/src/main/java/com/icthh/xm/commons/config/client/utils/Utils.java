package com.icthh.xm.commons.config.client.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class Utils {
    public static <T> List<T> nullSafeList(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }

    public static <T> Function<List<T>, List<T>> nullSafeList() {
        return Utils::nullSafeList;
    }

    public static <K, V> Map<K, V> nullSafeMap(Map<K, V> map) {
        return map == null ? new HashMap<>() : map;
    }

    public static <K, V> Function<Map<K, V>, Map<K, V>> nullSafeMap() {
        return Utils::nullSafeMap;
    }
}
