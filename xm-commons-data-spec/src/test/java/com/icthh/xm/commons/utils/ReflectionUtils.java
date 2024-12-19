package com.icthh.xm.commons.utils;

import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static com.icthh.xm.commons.utils.TestConstants.TEST_TENANT;

@UtilityClass
public class ReflectionUtils {

    public static void setFieldValue(Object obj, String fieldName, Map<String, Object> tenantValueMap) throws NoSuchFieldException, IllegalAccessException {
        getSuperClassField(obj, fieldName).set(obj, new HashMap<>(Map.of(TEST_TENANT, new HashMap<>(tenantValueMap))));
    }

    public static Field getSuperClassField(Object obj, String fieldName) throws NoSuchFieldException {
        Field field = obj.getClass().getSuperclass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    public static Field getField(Object obj, String fieldName) throws NoSuchFieldException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }
}
