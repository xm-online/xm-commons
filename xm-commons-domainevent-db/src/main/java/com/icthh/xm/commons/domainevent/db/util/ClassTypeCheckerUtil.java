package com.icthh.xm.commons.domainevent.db.util;

import jakarta.persistence.Entity;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;
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

    public static boolean isCollectionOrAssociation(Object value) {
        if (value == null) {
            return false;
        }

        return value instanceof HibernateProxy
                || value instanceof PersistentCollection
                || value instanceof Collection
                || value instanceof Map
                || value.getClass().isArray();
    }
}
