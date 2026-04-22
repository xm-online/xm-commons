package com.icthh.xm.commons.migration.db.util;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@UtilityClass
public class CollectionsUtils {

    public static <T> T firstOrNull(List<T> list) {
        return Optional.ofNullable(list)
            .stream()
            .flatMap(Collection::stream)
            .findFirst()
            .orElse(null);
    }
}
