package com.icthh.xm.commons.privilege;

import com.icthh.xm.commons.domain.spec.FunctionSpec;
import com.icthh.xm.commons.permission.service.custom.CustomPrivilegesExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

@Slf4j
@Component
public class DynamicCustomPrivilegesExtractor implements CustomPrivilegesExtractor<FunctionSpec> {

    private final String appName;
    private final boolean enabled;
    private static final String SECTION_NAME = "-functions";
    private static final String PRIVILEGE_PREFIX = "FUNCTION.CALL.";

    public DynamicCustomPrivilegesExtractor(@Value("${spring.application.name}") String appName,
                                            @Value("${application.dynamic-permission-check.enabled:true}") boolean enabled) {
        this.appName = appName;
        this.enabled = enabled;
    }

    @Override
    public String getSectionName() {
        return appName + SECTION_NAME;
    }

    @Override
    public String getPrivilegePrefix() {
        return PRIVILEGE_PREFIX;
    }

    @Override
    public List<String> toPrivilegesList(Collection<FunctionSpec> specs) {
        return specs.stream()
            .filter(Objects::nonNull)
            .map(FunctionSpec::getKey)
            .filter(Objects::nonNull)
            .distinct()
            .collect(toList());
    }

    @Override
    public boolean isEnabled(String tenantKey) {
        return enabled;
    }
}
