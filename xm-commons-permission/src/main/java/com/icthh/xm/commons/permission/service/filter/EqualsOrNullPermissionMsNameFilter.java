package com.icthh.xm.commons.permission.service.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(name = "permissionMsNameFilter")
public class EqualsOrNullPermissionMsNameFilter implements PermissionMsNameFilter {

    @Value("${spring.application.name}")
    private String msName;

    @Override
    public boolean filterPermission(String permissionMsName) {
        return StringUtils.isBlank(msName) || StringUtils.equals(permissionMsName, msName);
    }
}
