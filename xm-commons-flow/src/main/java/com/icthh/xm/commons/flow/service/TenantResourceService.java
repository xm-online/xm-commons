package com.icthh.xm.commons.flow.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.flow.domain.TenantResource;
import com.icthh.xm.commons.flow.service.TenantResourceConfigService.TenantResourceConfig;
import com.icthh.xm.commons.flow.spec.resource.TenantResourceTypeService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Slf4j
@RequiredArgsConstructor
@Component
public class TenantResourceService {

    private final TenantResourceConfigService tenantResourceConfigService;
    private final TenantResourceTypeService resourceTypeService;
    private final TenantConfigRepository tenantConfigRepository;
    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerModule(new JavaTimeModule());

    public TenantResource getResource(String resourceKey) {
        return tenantResourceConfigService.getByKey(resourceKey);
    }

    public List<TenantResource> getResources(String resourceType) {
        if (StringUtils.isBlank(resourceType)) {
            return tenantResourceConfigService.resources();
        } else {
            return tenantResourceConfigService.getByResourceType(resourceType);
        }
    }

    public void createResource(TenantResource resource) {
        assertNotExits(resource);
        modifyTenantResource(resource);
    }

    public void updateResource(TenantResource resource) {
        assertExits(resource.getKey());
        modifyTenantResource(resource);
    }

    private void modifyTenantResource(TenantResource resource) {
        assertResourceType(resource.getResourceType());
        Map<String, TenantResourceConfig> updatedConfigs = new HashMap<>();
        resource.setUpdateDate(Instant.now());
        Map<String, TenantResourceConfig> configFiles = tenantResourceConfigService.copyFilesConfig();
        var configsWhereRemovedResource = tenantResourceConfigService.removeResource(configFiles, resource.getKey());
        var configsWhereAddedResource = tenantResourceConfigService.updateFileConfiguration(configFiles, resource);
        updatedConfigs.putAll(configsWhereRemovedResource);
        updatedConfigs.putAll(configsWhereAddedResource);
        updateConfigurations(updatedConfigs);
    }

    private void assertResourceType(String resourceType) {
        if (resourceTypeService.getResource(resourceType) == null) {
            throw new BusinessException("error.resource.type.not.found", "Resource type " + resourceType + " not found");
        }
    }

    public void deleteResource(String resourceKey) {
        assertExits(resourceKey);
        // TODO check flows, avoid delete used resource
        Map<String, TenantResourceConfig> configFiles = tenantResourceConfigService.copyFilesConfig();
        var configsWhereRemovedResource = tenantResourceConfigService.removeResource(configFiles, resourceKey);
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
