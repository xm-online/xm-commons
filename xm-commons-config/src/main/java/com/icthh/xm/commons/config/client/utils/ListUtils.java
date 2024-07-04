package com.icthh.xm.commons.config.client.utils;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public final class ListUtils {
    public static <T> List<T> nullSafeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    public static <T> Function<List<T>, List<T>> nullSafeList() {
        return ListUtils::nullSafeList;
    }
}
