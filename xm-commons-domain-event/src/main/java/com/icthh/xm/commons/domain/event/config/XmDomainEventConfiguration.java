package com.icthh.xm.commons.domain.event.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.domain.event.service.Transport;
import com.icthh.xm.commons.migration.db.liquibase.LiquibaseRunner;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;

@Slf4j
@Component
public class XmDomainEventConfiguration implements RefreshableConfiguration {

    private static final String OUTBOX_CHANGE_LOG_PATH
        = "classpath:config/liquibase/changelog/outbox/db.changelog-master.yaml";
    private static final String TENANT_NAME = "tenant";

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());
    private final TenantContextHolder tenantContextHolder;
    private final LiquibaseRunner liquibaseRunner;
    private final ApplicationContext applicationContext;
    private final String configPath;

    private final Map<String, EventPublisherConfig> configByTenant = new HashMap<>();
    //<tenant, <source, transport>>
    private final Map<String, Map<String, Transport>> transportBySource = new WeakHashMap<>();


    public XmDomainEventConfiguration(@Value("${spring.application.name}") String appName,
                                      TenantContextHolder tenantContextHolder,
                                      LiquibaseRunner liquibaseRunner,
                                      ApplicationContext applicationContext) {
        this.tenantContextHolder = tenantContextHolder;
        this.configPath = "/config/tenants/{tenant}/" + appName + "/domain-events.yml";
        this.liquibaseRunner = liquibaseRunner;
        this.applicationContext = applicationContext;
    }

    public SourceConfig getSourceConfig(String source) {
        return getEventPublisherConfig().getSources().get(source);
    }

    public PublisherConfig getPublisherConfig(String source) {
        return getEventPublisherConfig().getPublisher();
    }

    public Transport getTransport(String source) {
        String tenantKey = tenantContextHolder.getTenantKey();
        return Optional
            .ofNullable(transportBySource.get(tenantKey))
            .map(sourceTransportMap -> sourceTransportMap.get(source))
            .orElseThrow(() -> new IllegalStateException(
                String.format("Transport is not configured for tenant: %s and source: %s", tenantKey, source))
            );
    }

    private EventPublisherConfig getEventPublisherConfig() {
        String tenantKey = tenantContextHolder.getTenantKey();
        EventPublisherConfig config = configByTenant.get(tenantKey);
        Objects.requireNonNull(config, String.format("EventPublisherConfig does not exists for tenant: %s", tenantKey));
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

        if (publisherConfig != null && publisherConfig.isEnabled()) {
            configByTenant.put(tenantKey, publisherConfig);
            initTransportSourceMap(tenantKey, publisherConfig.getSources());
            liquibaseRunner.runOnTenant(tenantKey, OUTBOX_CHANGE_LOG_PATH);
        } else {
            configByTenant.remove(tenantKey);
        }
    }

    private void initTransportSourceMap(String tenantKey, Map<String, SourceConfig> sourceConfigMap) {
        Map<String, Transport> sourceTransportMap = transportBySource.computeIfAbsent(tenantKey, k -> new HashMap<>());
        sourceTransportMap.clear();
        sourceConfigMap.forEach((s, sourceConfig) -> {
            if (sourceConfig.isEnabled()) {
                sourceTransportMap.put(s, applicationContext.getBean(sourceConfig.getTransport(), Transport.class));
            }
        });
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
