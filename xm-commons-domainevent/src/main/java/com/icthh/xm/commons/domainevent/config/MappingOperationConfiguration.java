package com.icthh.xm.commons.domainevent.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.domainevent.domain.MsMappingConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
@Component
public class MappingOperationConfiguration implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenant";

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());
    private ConcurrentHashMap<String, Map<String, MsMappingConfig>> tenantConfig = new ConcurrentHashMap<>();

    private final String configPath;

    public MappingOperationConfiguration() {
        this.configPath = "/config/tenants/{tenant}/mapping-operation-config.yml";
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        setConfig(updatedKey, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(configPath, updatedKey);
    }

    private void setConfig(String updatedKey, String config) {
        if (StringUtils.isEmpty(config)) {
            return;
        }

        Map<String, MsMappingConfig> configMap = readConfig(updatedKey, config);
        if (configMap != null && !configMap.isEmpty()) {
            String tenantKey = extractTenant(updatedKey);
            tenantConfig.put(tenantKey, configMap);
        }
    }

    private String extractTenant(String updatedKey) {
        return matcher.extractUriTemplateVariables(configPath, updatedKey).get(TENANT_NAME);
    }

    private Map<String, MsMappingConfig> readConfig(String updatedKey, String config) {
        Map<String, MsMappingConfig> configMap = null;
        try {
            configMap = ymlMapper.readValue(config, new TypeReference<Map<String, MsMappingConfig>>() {
            });
        } catch (Exception e) {
            log.error("Error reading mapping config from path: {}", updatedKey, e);
        }
        return configMap;
    }

    public String getOperationMapping(String tenantKey, String appName, String method, String url) {
        Map<String, MsMappingConfig> configMap = tenantConfig.get(tenantKey);
        if (configMap == null) {
            return getOperation(method, null, url);
        }

        MsMappingConfig msMappingConfig = configMap.get(appName);
        if (msMappingConfig == null) {
            return getOperation(method, null, url);
        }

        return getOperation(method, msMappingConfig, url);
    }

    private String getOperation(String method, MsMappingConfig msMappingConfig, String url) {
        switch (method) {
            case "GET":
                return getOperation(url, msMappingConfig, MsMappingConfig.OperationMapping::getGetMethod, "viewed");
            case "POST":
                return getOperation(url, msMappingConfig, MsMappingConfig.OperationMapping::getPosMethod, "created");
            case "PUT":
                return getOperation(url, msMappingConfig, MsMappingConfig.OperationMapping::getPutMethod, "changed");
            case "DELETE":
                return getOperation(url, msMappingConfig, MsMappingConfig.OperationMapping::getDeleteMethod, "deleted");
            default:
                return "";
        }
    }

    private String getOperation(String url, MsMappingConfig msMappingConfig,
                                Function<MsMappingConfig.OperationMapping, MsMappingConfig.MethodMapping> getGetMethod, String defaultValue) {

        List<MsMappingConfig.Mapping> mappings = Optional.ofNullable(msMappingConfig)
            .map(MsMappingConfig::getOperationMapping)
            .map(getGetMethod)
            .map(MsMappingConfig.MethodMapping::getMappings)
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
            .orElse(defaultValue);
    }

}
