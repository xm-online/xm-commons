package com.icthh.xm.commons.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.domain.SpecWithDefinitionAndForm;
import com.icthh.xm.commons.enums.SpecPathPatternEnum;
import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.service.SpecificationProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.icthh.xm.commons.ObjectMapperUtils.readSpecYml;
import static com.icthh.xm.commons.enums.SpecPathPatternEnum.JSON_CONFIG_PATH_PATTERN;

/**
 * This an abstraction for specification files processing located directly in .../{spec}.yml, .../{spec}/*.yml
 * and .../{spec}/**\/*.json
 * Processing specification should extend {@link SpecWithDefinitionAndForm} interface.
 * @param <S> specification to process
 */
@Slf4j
@IgnoreLogginAspect
public abstract class DataSpecificationService<S extends SpecWithDefinitionAndForm> implements RefreshableConfiguration {

    private final Class<S> specType;
    private final ObjectMapper objectMapper;
    private final JsonListenerService jsonListenerService;
    private final SpecificationProcessingService<S> specProcessingService;

    private final Map<String, Map<String, S>> specsByTenant; // tenantKey -> specPath -> spec object
    private final Map<String, Map<String, String>> specFilesByTenant; // tenantKey -> specPath -> spec file content

    public DataSpecificationService(Class<S> specType, JsonListenerService jsonListenerService,
                                    SpecificationProcessingService<S> specProcessingService) {
        this.specType = specType;
        this.jsonListenerService = jsonListenerService;
        this.specProcessingService = specProcessingService;
        this.objectMapper = new ObjectMapper(new YAMLFactory());
        this.specsByTenant = new ConcurrentHashMap<>();
        this.specFilesByTenant = new ConcurrentHashMap<>();
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        try {
            String tenant = SpecPathPatternEnum.findTenantName(updatedKey, folder());
            switch (SpecPathPatternEnum.getByPath(updatedKey, folder()).get()) {
                case SPEC_PATH_PATTERN, SPEC_FOLDER_PATH_PATTERN:
                    processYmlSpec(tenant, updatedKey, config);
                    break;
                case JSON_CONFIG_PATH_PATTERN:
                    processJsonSpec(tenant, updatedKey, config);
                    break;
            }
        } catch (Exception e) {
            log.error("Error when update spec by path {} ", updatedKey, e);
        }
    }

    private void processYmlSpec(String tenant, String updatedKey, String config) throws JsonProcessingException {
        if (StringUtils.isBlank(config)) {
            specFilesByTenant.get(tenant).remove(updatedKey);
            specsByTenant.get(tenant).remove(updatedKey);
            log.info("Spec for tenant {} was removed due to empty", tenant);

        } else {
            specFilesByTenant.computeIfAbsent(tenant, key -> new LinkedHashMap<>()).put(updatedKey, config);
            S specification = (S) specProcessingService.processSpecification(tenant, specKey(), objectMapper.readValue(config, specType));
            specsByTenant.computeIfAbsent(tenant, key -> new LinkedHashMap<>()).put(updatedKey, specification);
            log.info("Updated spec by tenant {} and file {}", tenant, updatedKey);
        }
    }

    private void processJsonSpec(String tenant, String updatedKey, String config) {
        String relativePath = updatedKey.substring(updatedKey.indexOf(specKey()));
        jsonListenerService.processTenantSpecification(tenant, relativePath, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return SpecPathPatternEnum.getByPath(updatedKey, folder()).isPresent();
    }

    @Override
    public void refreshFinished(Collection<String> paths) {
        paths.stream()
            .filter(p -> JSON_CONFIG_PATH_PATTERN.match(p, folder()))
            .map(p -> JSON_CONFIG_PATH_PATTERN.getTenantName(p, folder()))
            .forEach(t -> specProcessingService.updateByTenantState(t, specKey(), getSpecificationsFromFiles(t)));
    }

    public Map<String, S> getTenantSpecifications(String tenant) {
        return specsByTenant.getOrDefault(tenant, Map.of());
    }

    private Set<S> getSpecificationsFromFiles(String tenantKey) {
        return specFilesByTenant.getOrDefault(tenantKey, Map.of()).values().stream()
            .map(config -> readSpecYml(tenantKey, config, specType))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
    }
    /**
     * Define the specification key {spec} (e.g. myspec)
     * @return  specification key
     */
    public abstract String specKey();

    /**
     * Define the folder path where your {spec}.yml and /{spec} directory exits (e.g. myspec, /service/myspec)
     * @return  folder path
     */
    public abstract String folder();
}
