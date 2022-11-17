package com.icthh.xm.commons.domain.event.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class XmDomainEventConfiguration implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenant";

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());
    private final TenantContextHolder tenantContextHolder;
    private final LiquibaseRunner liquibaseRunner;
    private final String configPath;

    private final Map<String, EventPublisherConfig> configByTenant = new HashMap<>();


    public XmDomainEventConfiguration(@Value("${spring.application.name}") String appName,
                                      TenantContextHolder tenantContextHolder,
                                      LiquibaseRunner liquibaseRunner) {
        this.tenantContextHolder = tenantContextHolder;
        this.configPath = "/config/tenants/{tenant}/" + appName + "/domain-events.yml";
        this.liquibaseRunner = liquibaseRunner;
    }

    public SourceConfig getInterceptorConfig(String source) {
        return getEventPublisherConfig().getSources().get(source);
    }

    public PublisherConfig getPublisherConfig(String source) {
        return getEventPublisherConfig().getPublisher();
    }

    private EventPublisherConfig getEventPublisherConfig() {
        String tenantKey = tenantContextHolder.getTenantKey();
        EventPublisherConfig config = configByTenant.get(tenantKey);
        if (config == null) {
            throw new IllegalStateException(
                String.format("EventPublisherConfig does not exists for tenant: %s", tenantKey)
            );
        }
        return config;
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        setConfig(updatedKey, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(configPath, updatedKey);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        if (isListeningConfiguration(configKey)) {
            setConfig(configKey, configValue);
        }
    }

    private void setConfig(String updatedKey, String config) {
        String tenantKey = extractTenant(updatedKey);
        if (StringUtils.isEmpty(config)) {
            return;
        }
        EventPublisherConfig publisherConfig = readConfig(updatedKey, config);

        if (publisherConfig.isEnabled()) {
            configByTenant.put(tenantKey, publisherConfig);
            liquibaseRunner.runOnTenant(tenantKey);
        } else {
            configByTenant.remove(tenantKey);
        }
    }

    private String extractTenant(final String updatedKey) {
        return matcher.extractUriTemplateVariables(configPath, updatedKey).get(TENANT_NAME);
    }

    private EventPublisherConfig readConfig(String updatedKey, String config) {
        EventPublisherConfig publisherConfig = null;
        try {
            publisherConfig = ymlMapper.readValue(config, EventPublisherConfig.class);
        } catch (Exception e) {
            log.error("Error reading event publisher config from path: {}", updatedKey, e);
        }
        return publisherConfig;
    }
}
