package com.icthh.xm.commons.flow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.flow.domain.TenantResource;
import com.icthh.xm.commons.flow.service.TenantResourceConfigService.TenantResourceConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Slf4j
@RequiredArgsConstructor
@Component
public class TenantResourceService {

    private final TenantResourceConfigService tenantResourceConfigService;
    private final TenantConfigRepository tenantConfigRepository;
    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory()).registerModule(new JavaTimeModule());

    public TenantResource getResource(String resourceKey) {
        return tenantResourceConfigService.getByKey(resourceKey);
    }

    public List<TenantResource> getResources(String resourceType) {
        return tenantResourceConfigService.getByResourceType(resourceType);
    }

    public void createResource(TenantResource resource) {
        assertNotExits(resource);
        updateResource(resource);
    }

    public void updateResource(TenantResource resource) {
        assertExits(resource.getKey());
        Map<String, TenantResourceConfig> updatedConfigs = new HashMap<>();
        var configsWhereRemovedResource = tenantResourceConfigService.removeResource(resource.getKey());
        var configsWhereAddedResource = tenantResourceConfigService.updateFileConfiguration(resource);
        updatedConfigs.putAll(configsWhereRemovedResource);
        updatedConfigs.putAll(configsWhereAddedResource);

        updateConfigurations(updatedConfigs);
    }

    public void deleteResource(String resourceKey) {
        assertExits(resourceKey);
        var configsWhereRemovedResource = tenantResourceConfigService.removeResource(resourceKey);
        updateConfigurations(configsWhereRemovedResource);
    }

    private void updateConfigurations(Map<String, TenantResourceConfig> updatedConfigs) {
        log.debug("Updated configs: {}", updatedConfigs);
        log.info("Updated configs.size: {}", updatedConfigs.size());
        List<Configuration> configurations = updatedConfigs.entrySet().stream()
            .map(entry -> new Configuration(entry.getKey(), writeConfig(entry.getValue())))
            .collect(toList());
        tenantConfigRepository.updateConfigurations(configurations);
    }

    @SneakyThrows
    private String writeConfig(TenantResourceConfig config) {
        return objectMapper.writeValueAsString(config);
    }

    private void assertNotExits(TenantResource resource) {
        if (tenantResourceConfigService.getByKey(resource.getKey()) != null) {
            throw new BusinessException("error.resource.already.exists", "Resource with key " + resource.getKey() + " already exists");
        }
    }

    private void assertExits(String resourceKey) {
        if (tenantResourceConfigService.getByKey(resourceKey) == null) {
            throw new BusinessException("error.resource.not.found", "Resource with key " + resourceKey + " not found");
        }
    }
}
