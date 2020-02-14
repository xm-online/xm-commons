package com.icthh.xm.commons.permission.inspector.scanner;

import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.permission.domain.Privilege;
import java.lang.annotation.Annotation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.nonNull;

@Component
@Slf4j
@RequiredArgsConstructor
public class PrivilegeScanner {

    private final Reflections reflections;

    @Value("${spring.application.name}")
    private String appName;

    /**
     * Scan for {@link PreAuthorize} and {@link PostAuthorize} annotations
     * and create {@link Set} of {@link Privilege}.
     *
     * @return set of privileges
     */
    public Set<Privilege> scan() {
        StopWatch stopWatch = StopWatch.createStarted();

        Set<Privilege> privileges = new HashSet<>();
        Set<Method> securedMethods = new HashSet<>();
        loadMethods(securedMethods, PreAuthorize.class);
        loadMethods(securedMethods, PostAuthorize.class);
        loadMethods(securedMethods, FindWithPermission.class);
        loadMethods(securedMethods, PostFilter.class);

        for (Method method: securedMethods) {
            PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
            PostAuthorize postAuthorize = method.getAnnotation(PostAuthorize.class);
            FindWithPermission findWithPermission = method.getAnnotation(FindWithPermission.class);
            PostFilter postFilter = method.getAnnotation(PostFilter.class);
            Privilege privilege = new Privilege();

            if (nonNull(preAuthorize)) {
                privilege = parse(preAuthorize.value());
            }

            if (nonNull(postAuthorize)) {
                privilege = parse(postAuthorize.value());
            }

            if (nonNull(findWithPermission)) {
                privilege = parse(findWithPermission.value());
                privilege.getResources().add("returnObject");
            }

            if (nonNull(postFilter)) {
                privilege = parse(postFilter.value());
                privilege.getResources().add("returnObject");
            }

            if (nonNull(privilege.getKey())) {
                updateCustomPrivilege(method, privilege);
                privileges.add(privilege);
            }
        }

        log.info("Found {} privileges in {} ms", privileges.size(), stopWatch.getTime());
        return privileges;
    }

    private void updateCustomPrivilege(Method method, Privilege privilege) {
        if (method.isAnnotationPresent(PrivilegeDescription.class)) {
            String customDescription = method.getAnnotation(PrivilegeDescription.class).value();
            if (StringUtils.isNotEmpty(customDescription)) {
                privilege.setCustomDescription(customDescription);
            }
        } else {
            log.warn("Privilege [{}] does not have description. Add annotation @PrivilegeDescription to: {}",
                     privilege.getKey(), method);
        }
    }

    private void loadMethods(Set<Method> securedMethods, Class<? extends Annotation> annotation) {
        securedMethods.addAll(reflections.getMethodsAnnotatedWith(annotation));
    }

    private Privilege parse(String expression) {
        Privilege privilege = new Privilege();
        privilege.setMsName(appName);
        String[] inputParams = StringUtils.split(expression, ",");

        for (int i = 0; i < inputParams.length; i++) {
            if (i == inputParams.length - 1) {
                if (StringUtils.contains(inputParams[i], "'")) {
                    privilege.setKey(StringUtils.substringBetween(inputParams[i], "'")
                        .replace("@msName", appName.toUpperCase()));
                } else {
                    privilege.setKey(inputParams[i].replace("@msName", appName.toUpperCase()));
                }
            } else {
                String resource = StringUtils.substringBetween(inputParams[i], "'");
                if (StringUtils.isNotEmpty(resource)) {
                    privilege.getResources().add(resource);
                }
            }
        }
        return privilege;
    }

}
