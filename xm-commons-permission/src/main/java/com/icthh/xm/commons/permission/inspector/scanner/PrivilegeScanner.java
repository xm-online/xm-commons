package com.icthh.xm.commons.permission.inspector.scanner;

import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.domain.Privilege;
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

import java.util.Set;
import java.util.stream.Collectors;

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
        Set<Privilege> preAuthPrivileges = reflections.getMethodsAnnotatedWith(PreAuthorize.class).stream()
            .map(element -> element.getAnnotation(PreAuthorize.class))
            .map(PreAuthorize::value)
            .map(this::parse)
            .collect(Collectors.toSet());

        Set<Privilege> postAuthPrivileges = reflections.getMethodsAnnotatedWith(PostAuthorize.class).stream()
            .map(element -> element.getAnnotation(PostAuthorize.class))
            .map(PostAuthorize::value)
            .map(this::parse)
            .collect(Collectors.toSet());

        Set<Privilege> findPrivileges = reflections.getMethodsAnnotatedWith(FindWithPermission.class).stream()
            .map(element -> element.getAnnotation(FindWithPermission.class))
            .map(FindWithPermission::value)
            .map(this::parse)
            .peek(privilege -> privilege.getResources().add("returnObject"))
            .collect(Collectors.toSet());

        Set<Privilege> postFilterPrivileges = reflections.getMethodsAnnotatedWith(PostFilter.class).stream()
            .map(element -> element.getAnnotation(PostFilter.class))
            .map(PostFilter::value)
            .map(this::parse)
            .peek(privilege -> privilege.getResources().add("returnObject"))
            .collect(Collectors.toSet());

        preAuthPrivileges.addAll(postAuthPrivileges);
        preAuthPrivileges.addAll(findPrivileges);
        preAuthPrivileges.addAll(postFilterPrivileges);
        log.info("Found {} privileges in {} ms", preAuthPrivileges.size(), stopWatch.getTime());
        return preAuthPrivileges;
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
