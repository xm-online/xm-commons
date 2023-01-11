package com.icthh.xm.commons.domainevent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.domainevent.config.event.InitSourceEventPublisher;
import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventSource;
import com.icthh.xm.commons.domainevent.service.Transport;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
@Component
public class XmDomainEventConfiguration implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenant";

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());
    private final TenantContextHolder tenantContextHolder;
    private final InitSourceEventPublisher initSourceEventPublisher;
    private final ApplicationContext applicationContext;
    private final String configPath;

    private final Map<String, EventPublisherConfig> configByTenant = new HashMap<>();
    //<tenant, <source, transport>>
    private final Map<String, Map<String, Transport>> transportBySource = new HashMap<>();
    //<tenant, TransformMappingConfig>>
    private final ConcurrentHashMap<String, TransformMappingConfig> operationMappingByTenant = new ConcurrentHashMap<>();


    public XmDomainEventConfiguration(@Value("${spring.application.name}") String appName,
                                      TenantContextHolder tenantContextHolder,
                                      InitSourceEventPublisher initSourceEventPublisher,
                                      ApplicationContext applicationContext) {
        this.tenantContextHolder = tenantContextHolder;
        this.configPath = "/config/tenants/{tenant}/" + appName + "/domainevent.yml";
        this.initSourceEventPublisher = initSourceEventPublisher;
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
        if (StringUtils.isEmpty(config)) {
            return;
        }
        String tenantKey = extractTenant(updatedKey);
        configByTenant.remove(tenantKey);
        EventPublisherConfig eventPublisherConfig = readConfig(updatedKey, config);
        if (eventPublisherConfig != null && eventPublisherConfig.isEnabled()) {
            configByTenant.put(tenantKey, eventPublisherConfig);

            Map<String, SourceConfig> sources = eventPublisherConfig.getSources();
            initTransportSourceMap(tenantKey, sources);
            initSourceEventPublisher.publish(tenantKey, sources.values());

            SourceConfig sourceConfig = sources.get(DefaultDomainEventSource.WEB.getCode());
            if (sourceConfig == null || !sourceConfig.isEnabled()) {
                return;
            }

            TransformMappingConfig transform = sourceConfig.getTransform();
            if (transform == null) {
                return;
            }
            operationMappingByTenant.put(tenantKey, transform);
        }
    }

    private void initTransportSourceMap(String tenantKey, Map<String, SourceConfig> sourceConfigMap) {
        Map<String, Transport> sourceTransportMap = new HashMap<>();
        transportBySource.put(tenantKey, sourceTransportMap);
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

    public String getOperationMapping(String tenantKey, String method, String url) {
        TransformMappingConfig transformMappingConfig = operationMappingByTenant.get(tenantKey);
        return getOperation(method, transformMappingConfig, url);
    }

    private String getOperation(String method, TransformMappingConfig transformMappingConfig, String url) {
        switch (method) {
            case "GET":
                return getOperation(url, transformMappingConfig, TransformMappingConfig.OperationMapping::getGetMethod, "viewed");
            case "POST":
                return getOperation(url, transformMappingConfig, TransformMappingConfig.OperationMapping::getPosMethod, "created");
            case "PUT":
                return getOperation(url, transformMappingConfig, TransformMappingConfig.OperationMapping::getPutMethod, "changed");
            case "DELETE":
                return getOperation(url, transformMappingConfig, TransformMappingConfig.OperationMapping::getDeleteMethod, "deleted");
            default:
                return "";
        }
    }

    private String getOperation(String url, TransformMappingConfig transformMappingConfig,
                                Function<TransformMappingConfig.OperationMapping, TransformMappingConfig.MethodMapping> operationMappingFunction,
                                String defaultValue) {

        List<TransformMappingConfig.Mapping> mappings = Optional.ofNullable(transformMappingConfig)
            .map(TransformMappingConfig::getOperationMapping)
            .map(operationMappingFunction)
            .map(TransformMappingConfig.MethodMapping::getMappings)
            .orElse(List.of());

        return mappings.stream()
            .filter(it -> matcher.match(it.getUrlPattern(), url))
            .findFirst()
            .map(it -> {
                String operationText = it.getName();
                Map<String, String> variables = matcher.extractUriTemplateVariables(it.getUrlPattern(), url);
                for (Map.Entry<String, String> e : variables.entrySet()) {
                    operationText = operationText.replace("{" + e.getKey() + "}", e.getValue());
                }

                return operationText;
            })
            .orElse(getDefaultOperation(url, defaultValue));
    }

    private String getDefaultOperation(String url, String defaultValue) {
        return getResourceName(url) + " " + defaultValue;
    }

    private String getResourceName(String path) {
        String name = StringUtils.removeStart(path, "/api/");
        if (StringUtils.startsWith(name, "_search")) {
            name = StringUtils.substringAfter(name, "/");
        }
        return StringUtils.defaultIfBlank(StringUtils.substringBefore(name, "/"), "unknown");
    }
}
