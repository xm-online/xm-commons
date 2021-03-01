package com.icthh.xm.commons.permission.utils;

import java.util.Collection;
import java.util.HashSet;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CollectionUtils {

    public static <T> boolean listsNotEqualsIgnoreOrder(Collection<T> collection1, Collection<T> collection2) {
        return new HashSet<>(collection1).equals(new HashSet<>(collection2));
    }
}
