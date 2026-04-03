package com.icthh.xm.commons.config.client.api.refreshable;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.type.TypeFactory;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.springframework.beans.factory.annotation.Value;

public abstract class SimpleConfigItemClassRefreshableConfiguration<CONFIG, CONFIG_FILE> extends AbstractRefreshableConfiguration<CONFIG, CONFIG_FILE> {

    public SimpleConfigItemClassRefreshableConfiguration(@Value("${spring.application.name}") String appName,
                                                         TenantContextHolder tenantContextHolder)  {
        super(appName, tenantContextHolder);
    }

    public JavaType configFileJavaType(TypeFactory factory) {
        return factory.constructType(configFileClass());
    }

    public abstract Class<CONFIG_FILE> configFileClass();

}
