package com.icthh.xm.commons.lep.api;

import com.icthh.xm.commons.logging.util.BasePackageDetector;
import lombok.SneakyThrows;
import org.reflections.Reflections;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class LepContextMapSupport {
    private final Map<Class<? extends BaseLepContext>, Map<String, VarHandle>> classFieldHandles = new HashMap<>();

    public LepContextMapSupport(BasePackageDetector basePackageDetector) {
        this(basePackageDetector.getBasePackage());
    }

    public LepContextMapSupport(String basePackage) {
        Reflections reflections = new Reflections(basePackage);
        Set<Class<? extends BaseLepContext>> classes = reflections.getSubTypesOf(BaseLepContext.class);
        initializeVarHandles(classes);
    }

    @SneakyThrows
    private void initializeVarHandles(Set<Class<? extends BaseLepContext>> classes) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        for (Class<? extends BaseLepContext> type : classes) {
            Map<String, VarHandle> fieldHandles = new HashMap<>();
            for (Field field : getFields(type)) {
                VarHandle handle = lookup.unreflectVarHandle(field);
                fieldHandles.put(field.getName(), handle);
            }
            classFieldHandles.put(type, fieldHandles);
        }
    }

    private List<Field> getFields(Class<? extends BaseLepContext> type) {
        return Stream.of(type.getFields())
            .filter(not(Field::isSynthetic))
            .filter(it -> !Modifier.isStatic(it.getModifiers()))
            .collect(Collectors.toList());
    }

    public Object get(String fieldName, BaseLepContext instance) {
        VarHandle handle = classFieldHandles.get(instance.getClass()).get(fieldName);
        return handle == null ? null : handle.get(instance);
    }
}
