package com.icthh.xm.commons.config.client.api.refreshable;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Slf4j
public abstract class SingleFileAbstractRefreshableConfiguration<CONFIG> extends AbstractRefreshableConfiguration<CONFIG, CONFIG> {

    public SingleFileAbstractRefreshableConfiguration(@Value("${spring.application.name}") String appName,
                                                      TenantContextHolder tenantContextHolder) {
        super(appName, tenantContextHolder);
    }

    public List<String> filesPathAntPatterns() {
        return List.of(buildFilePath("*"));
    }

    @Override
    public CONFIG joinTenantConfiguration(List<CONFIG> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    public JavaType configFileJavaType(TypeFactory factory) {
        return factory.constructType(configFileClass());
    }

    public abstract Class<CONFIG> configFileClass();

}
