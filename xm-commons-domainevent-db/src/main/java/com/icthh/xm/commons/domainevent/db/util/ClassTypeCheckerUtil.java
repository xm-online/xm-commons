package com.icthh.xm.commons.domainevent.db.util;

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
}
