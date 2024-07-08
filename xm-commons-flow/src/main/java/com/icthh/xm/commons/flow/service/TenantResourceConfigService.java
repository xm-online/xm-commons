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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableMap;

@Component
public class TenantResourceConfigService extends MapRefreshableConfiguration<TenantResource, TenantResourceConfig> {

    // tenant -> resourceType -> resourceKey -> resourceData (Map<String, Object> it`s data)
    private final Map<String, Map<String, Map<String, Map<String, Object>>>> resourcesDataByType = new ConcurrentHashMap<>();

    public TenantResourceConfigService(@Value("${spring.application.name}") String appName,
                                       TenantContextHolder tenantContextHolder) {
        super(appName, tenantContextHolder);
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
    public String folder() {
        return "/flow";
    }

    @Override
    public void onUpdate(String tenantKey, Map<String, TenantResource> configuration) {
        Map<String, List<TenantResource>> byType = configuration.values().stream()
            .collect(groupingBy(TenantResource::getResourceType));
        var resourcesDataByType = byType.keySet().stream().collect(toUnmodifiableMap(identity(), getGroupByKey(byType)));
        this.resourcesDataByType.put(tenantKey, resourcesDataByType);
    }

    private Function<String, Map<String, Map<String, Object>>> getGroupByKey(Map<String, List<TenantResource>> byType) {
        return type -> byType.get(type).stream().collect(toUnmodifiableMap(TenantResource::getKey, TenantResource::getData));
    }

    public List<TenantResource> resources() {
        Map<String, TenantResource> configuration = getConfiguration();
        return List.copyOf(configuration.values());
    }

    public TenantResource getByKey(String resourceKey) {
        return getConfiguration().get(resourceKey);
    }

    public List<TenantResource> getByResourceType(String resourceType) {
        return getConfiguration().values().stream()
            .filter(resource -> resource.getResourceType().equals(resourceType))
            .collect(toList());
    }

    public Map<String, TenantResourceConfig> removeResource(Map<String, TenantResourceConfig> configurationFiles,
                                                            String resourceKey) {
        Map<String, TenantResourceConfig> updateFiles = new HashMap<>();
        configurationFiles.forEach((file, config) -> {
            List<TenantResource> resources = config.getResources();
            boolean containsResource = resources.stream().anyMatch(it -> it.getKey().equals(resourceKey));
            if (containsResource) {
                updateFiles.put(file, config);
                resources.removeIf(it -> it.getKey().equals(resourceKey));
            }
        });
        return updateFiles;
    }

    public Map<String, TenantResourceConfig> updateFileConfiguration(Map<String, TenantResourceConfig> configurationFiles,
                                                                     TenantResource resource) {
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

    public Map<String, TenantResourceConfig>  copyFilesConfig() {
        Map<String, TenantResourceConfig> configurationFiles = getConfigurationFiles();
        return configurationFiles.entrySet().stream()
            .collect(toUnmodifiableMap(Entry::getKey, entry -> entry.getValue().copy()));
    }

    @Override
    public ObjectMapper buildObjectMapper() {
        return new ObjectMapper(new YAMLFactory())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(new JavaTimeModule());
    }

    public Map<String, Map<String, Map<String, Object>>> getResourcesDataByType() {
        return resourcesDataByType.get(tenantContextHolder.getTenantKey());
    }

    @Data
    public static class TenantResourceConfig {
        private List<TenantResource> resources;

        public List<TenantResource> getResources() {
            if (resources == null) {
                resources = new ArrayList<>();
            }
            return resources;
        }

        public TenantResourceConfig copy() {
            TenantResourceConfig tenantResourceConfig = new TenantResourceConfig();
            tenantResourceConfig.setResources(new ArrayList<>(getResources()));
            return tenantResourceConfig;
        }
    }

}
