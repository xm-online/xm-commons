package com.icthh.xm.commons.flow.service;

import com.icthh.xm.commons.config.client.api.refreshable.MapRefreshableConfiguration;
import com.icthh.xm.commons.flow.domain.TenantResource;
import com.icthh.xm.commons.flow.service.TenantResourceConfigService.TenantResourceConfig;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Component
public class TenantResourceConfigService extends MapRefreshableConfiguration<TenantResource, TenantResourceConfig> {

    private final String appName;
    private final TenantContextHolder tenantContextHolder;

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
}
