package com.icthh.xm.commons.domainevent.db.util;

import java.util.Collection;
import java.util.Map;
import org.springframework.beans.BeanUtils;

public class ClassTypeCheckerUtil {
    /**
     *
     * @param value the value to check
     * @return true if the value is a simple type, false otherwise
     */
    public static boolean isSimpleValue(Object value) {
        if (value == null) {
            return true;
        }

        return BeanUtils.isSimpleValueType(value.getClass());
    }

    public static boolean isCollections(Object value) {
        if (value == null) {
            return false;
        }

        return value instanceof Collection<?>
                || value instanceof Map<?, ?>
                || value.getClass().isArray();
    }
}
