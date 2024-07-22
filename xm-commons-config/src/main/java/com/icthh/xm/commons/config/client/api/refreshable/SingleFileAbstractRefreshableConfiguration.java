package com.icthh.xm.commons.config.client.api.refreshable;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
