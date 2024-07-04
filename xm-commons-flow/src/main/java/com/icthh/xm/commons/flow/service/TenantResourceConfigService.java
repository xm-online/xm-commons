package com.icthh.xm.commons.flow.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.config.client.api.refreshable.MapRefreshableConfiguration;
import com.icthh.xm.commons.flow.domain.TenantResource;
import com.icthh.xm.commons.flow.service.TenantResourceConfigService.TenantResourceConfig;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableMap;

@Component
public class TenantResourceConfigService extends MapRefreshableConfiguration<TenantResource, TenantResourceConfig> {

    private final String appName;
    private final TenantContextHolder tenantContextHolder;
    @Getter
    private volatile Map<String, Map<String, TenantResource>> resourcesByType = new HashMap<>();

    public TenantResourceConfigService(@Value("${spring.application.name}") String appName,
                                       TenantContextHolder tenantContextHolder) {
        super(appName, tenantContextHolder);
        this.appName = appName;
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    protected List<TenantResource> toConfigItems(TenantResourceConfig config) {
        return config.getResources();
    }

    @Override
    public Class<TenantResourceConfig> configFileClass() {
        return TenantResourceConfig.class;
    }

    @Override
    public String configName() {
        return "resources";
    }

    @Override
    public void onUpdate(Map<String, TenantResource> configuration) {
        Map<String, List<TenantResource>> byType = configuration.values().stream()
            .collect(groupingBy(TenantResource::getResourceType));
        this.resourcesByType = byType.keySet().stream().collect(toUnmodifiableMap(identity(), getGroupByKey(byType)));
    }

    private Function<String, Map<String, TenantResource>> getGroupByKey(Map<String, List<TenantResource>> byType) {
        return type -> byType.get(type).stream().collect(toUnmodifiableMap(TenantResource::getKey, identity()));
    }

    public List<TenantResource> resources() {
        Map<String, TenantResource> configuration = getConfiguration();
        return List.copyOf(configuration.values());
    }

    public TenantResource getByKey(String resourceKey) {
        return getConfiguration().get(resourceKey);
    }

    public Map<String, TenantResourceConfig> removeResource(String resourceKey) {
        Map<String, TenantResourceConfig> updateFiles = new HashMap<>();
        getConfigurationFiles().forEach((file, config) -> {
            List<TenantResource> resources = config.getResources();
            resources = resources == null ? List.of() : resources;
            boolean containsResource = resources.stream().anyMatch(it -> it.getKey().equals(resourceKey));
            if (containsResource) {
                updateFiles.put(file, config);
                resources.removeIf(it -> it.getKey().equals(resourceKey));
            }
        });
        return updateFiles;
    }

    public List<TenantResource> getByResourceType(String resourceType) {
        return getConfiguration().values().stream()
            .filter(resource -> resource.getResourceType().equals(resourceType))
            .collect(toList());
    }

    public Map<String, TenantResourceConfig> updateFileConfiguration(TenantResource resource) {
        Map<String, TenantResourceConfig> configurationFiles = getConfigurationFiles();
        String filePath = buildFilePath(resource.getKey());
        TenantResourceConfig resourceFile = configurationFiles.get(filePath);
        if (resourceFile != null) {
            resourceFile.getResources().add(resource);
        } else {
            resourceFile = new TenantResourceConfig();
            resourceFile.setResources(List.of(resource));
        }
        return Map.of(filePath, resourceFile);
    }

    private String buildFilePath(String resourceKey) {
        return "/config/tenants/" + tenantContextHolder.getTenantKey() + "/" + appName + "/" + configName() + "/" + resourceKey + ".yml";
    }

    @Data
    public static class TenantResourceConfig {
        private List<TenantResource> resources;
    }

    @Override
    public ObjectMapper buildObjectMapper() {
        return new ObjectMapper(new YAMLFactory())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(new JavaTimeModule());
    }
}
