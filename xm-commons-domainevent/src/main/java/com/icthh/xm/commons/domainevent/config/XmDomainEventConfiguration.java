package com.icthh.xm.commons.domainevent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.domainevent.config.event.InitSourceEventPublisher;
import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventSource;
import com.icthh.xm.commons.domainevent.service.Transport;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
    private final ConcurrentHashMap<String, List<TransformMappingConfig>> operationMappingByTenant = new ConcurrentHashMap<>();
    //<tenant, List<FilterConfig> filter>>
    private final ConcurrentHashMap<String, List<FilterConfig>> filterListByTenant = new ConcurrentHashMap<>();
    //<tenant, Set<String>>
    private final ConcurrentHashMap<String, Set<String>> headersSetByTenant = new ConcurrentHashMap<>();


    public XmDomainEventConfiguration(@Value("${spring.application.name}") String appName,
                                      TenantContextHolder tenantContextHolder,
                                      InitSourceEventPublisher initSourceEventPublisher,
                                      ApplicationContext applicationContext) {
        this.tenantContextHolder = tenantContextHolder;
        this.configPath = "/config/tenants/{tenant}/" + appName + "/domainevent.yml";
        this.initSourceEventPublisher = initSourceEventPublisher;
        this.applicationContext = applicationContext;
    }

    public DbSourceConfig getDbSourceConfig(String source) {
        DbSourceConfig dbSourceConfig = null;
        SourceConfig sourceConfig = getEventPublisherConfig().getSources().get(source);
        if (sourceConfig instanceof DbSourceConfig) {
            dbSourceConfig = (DbSourceConfig) sourceConfig;
        }
        return dbSourceConfig;
    }

    public WebSourceConfig getWebSourceConfig(String tenantKey, String source) {
        WebSourceConfig webSourceConfig = null;
        SourceConfig sourceConfig = getEventPublisherConfig(tenantKey).getSources().get(source);
        if (sourceConfig instanceof WebSourceConfig) {
            webSourceConfig = (WebSourceConfig) sourceConfig;
        }
        return webSourceConfig;
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

    private EventPublisherConfig getEventPublisherConfig(String tenantKey) {
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

            WebSourceConfig sourceConfig = getWebSourceConfig(tenantKey ,DefaultDomainEventSource.WEB.getCode());
            if (sourceConfig == null || !sourceConfig.isEnabled()) {
                return;
            }

            List<TransformMappingConfig> transformMappingConfigs = sourceConfig.getTransform();
            if (CollectionUtils.isNotEmpty(transformMappingConfigs)) {
                operationMappingByTenant.put(tenantKey, transformMappingConfigs);
            }

            List<FilterConfig> filter = sourceConfig.getFilter();
            if (CollectionUtils.isNotEmpty(filter)) {
                filterListByTenant.put(tenantKey, filter);
            }

            Set<String> sourceConfigHeaders = sourceConfig.getHeaders();
            if (CollectionUtils.isNotEmpty(sourceConfigHeaders)) {
                headersSetByTenant.put(tenantKey, sourceConfigHeaders);
            }
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

    public List<FilterConfig> getFilterListByTenant(String tenantKey) {
        return filterListByTenant.getOrDefault(tenantKey, List.of());
    }

    public Set<String> getTenantHeaders(String tenantKey) {
        return headersSetByTenant.getOrDefault(tenantKey, Set.of());
    }

    public String getOperationMapping(String tenantKey, String method, String url) {
        List<TransformMappingConfig> transformMappingConfigs = operationMappingByTenant.getOrDefault(tenantKey, List.of());
        return getOperationByUrl(method, transformMappingConfigs.stream(), url);
    }

    private String getOperationByUrl(String method, Stream<TransformMappingConfig> transformMappingConfigs, String url) {
        switch (method) {
            case "GET":
                return getOperationName(url, transformMappingConfigs.filter(createOperationPredicate("GET")), "viewed");
            case "POST":
                return getOperationName(url, transformMappingConfigs.filter(createOperationPredicate("POST")), "created");
            case "PUT":
                return getOperationName(url, transformMappingConfigs.filter(createOperationPredicate("PUT")), "changed");
            case "DELETE":
                return getOperationName(url, transformMappingConfigs.filter(createOperationPredicate("DELETE")), "deleted");
            default:
                return "";
        }
    }

    private Predicate<TransformMappingConfig> createOperationPredicate(String operationName) {
        return transformMappingConfig -> transformMappingConfig.getHttpOperation().contains(operationName);
    }

    private String getOperationName(String url, Stream<TransformMappingConfig> transformMappingConfigs,
                                    String defaultValue) {
        return transformMappingConfigs
            .filter(it -> matcher.match(it.getUrlPattern(), url))
            .findFirst()
            .map(it -> fillTemplate(url, it))
            .orElse(getDefaultOperation(url, defaultValue));
    }

    private String fillTemplate(final String url, final TransformMappingConfig it) {
        String operationText = it.getOperationName();
        Map<String, String> variables = matcher.extractUriTemplateVariables(it.getUrlPattern(), url);
        for (Map.Entry<String, String> e : variables.entrySet()) {
            operationText = operationText.replace("{" + e.getKey() + "}", e.getValue());
        }

        return operationText;
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
