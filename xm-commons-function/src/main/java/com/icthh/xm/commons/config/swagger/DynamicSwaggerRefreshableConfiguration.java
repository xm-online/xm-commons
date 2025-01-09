package com.icthh.xm.commons.config.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.icthh.xm.commons.utils.DataSpecConstants.TENANT_NAME;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Component
public class DynamicSwaggerRefreshableConfiguration implements RefreshableConfiguration {

    private final ObjectMapper objectMapper;
    private final AntPathMatcher matcher;
    private final String specPath;
    private final Map<String, DynamicSwaggerConfiguration> config;
    private final TenantContextHolder tenantContextHolder;

    public DynamicSwaggerRefreshableConfiguration(@Value("${spring.application.name}") String appName,
                                                  TenantContextHolder tenantContextHolder) {
        this.specPath = "/config/tenants/{tenantName}/" + appName + "/swagger.yml";
        this.tenantContextHolder = tenantContextHolder;
        this.objectMapper = new ObjectMapper(new YAMLFactory());
        this.matcher = new AntPathMatcher();
        this.config = new ConcurrentHashMap<>();
    }

    public void onRefresh(final String updatedKey, final String config) {
        try {
            String tenant = this.matcher.extractUriTemplateVariables(specPath, updatedKey).get(TENANT_NAME);
            if (isBlank(config)) {
                this.config.remove(tenant);
                log.info("Configuration was removed for tenant [{}] by key [{}]", tenant, updatedKey);
            } else {
                this.config.put(tenant, objectMapper.readValue(config, DynamicSwaggerConfiguration.class));
                log.info("Configuration was updated for tenant [{}] by key [{}]", tenant, updatedKey);
            }
        } catch (Exception e) {
            log.error("Error read configuration from path {}", updatedKey, e);
        }
    }

    public boolean isListeningConfiguration(final String updatedKey) {
        return this.matcher.match(specPath, updatedKey);
    }

    public DynamicSwaggerConfiguration getConfiguration() {
        return this.config.get(tenantContextHolder.getTenantKey());
    }
}
