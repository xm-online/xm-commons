package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.logging.util.BasePackageDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class DefaultLepContextActualClassDetector implements LepContextActualClassDetector {

    public static final String LEP_CONTEXT_DEFAULT_CLASS_NAME = "LepContext";

    private final BasePackageDetector basePackageDetector;

    public Class<? extends BaseLepContext> detectActualClass() {
        String basePackage = basePackageDetector.getBasePackage();
        Reflections reflections = new Reflections(basePackage);

        Set<Class<? extends BaseLepContext>> classes = reflections.getSubTypesOf(BaseLepContext.class);
        classes.remove(DefaultLepContext.class);
        removeParentClasses(classes);

        Class<? extends BaseLepContext> lepContextClass = DefaultLepContext.class;
        if (classes.size() == 1) {
            lepContextClass = classes.iterator().next();
        } else if (!classes.isEmpty()) {
            lepContextClass = classes.stream()
                .filter(this::isNameLepContext)
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Can't find LepContext class in base package: {} | variants: {}", basePackage, classes);
                    return new IllegalStateException("Can't find LepContext class in base package: " + basePackage);
                });
        }
        return lepContextClass;
    }

    private boolean isNameLepContext(Class<? extends BaseLepContext> type) {
        Class<?> currentClass = type;
        while (currentClass != null) {
            if (LEP_CONTEXT_DEFAULT_CLASS_NAME.equals(currentClass.getSimpleName())) {
                return true;
            }
            currentClass = currentClass.getSuperclass();
        }
        return false;
    }

    private void removeParentClasses(Set<Class<? extends BaseLepContext>> classList) {
        Set<Class<?>> classesToRemove = new HashSet<>();

        for (Class<?> type: classList) {
            for (Class<?> possibleChild: classList) {
                if (type != possibleChild && type.isAssignableFrom(possibleChild)) {
                    classesToRemove.add(type);
                    break;
                }
            }
        }

        classList.removeAll(classesToRemove);
    }
}
