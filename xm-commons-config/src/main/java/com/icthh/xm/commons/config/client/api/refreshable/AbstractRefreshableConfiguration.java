package com.icthh.xm.commons.config.client.api.refreshable;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractRefreshableConfiguration<CONFIG, CONFIG_FILE> implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";

    protected final Map<String, CONFIG> configurationsByTenant = new ConcurrentHashMap<>();
    protected final Map<String, Map<String, CONFIG_FILE>> configurationsByTenantByFile = new ConcurrentHashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper mapper = buildObjectMapper();

    private final TenantContextHolder tenantContextHolder;
    private final String appName;

    public AbstractRefreshableConfiguration(@Value("${spring.application.name}") String appName,
                                            TenantContextHolder tenantContextHolder) {
        this.tenantContextHolder = tenantContextHolder;
        this.appName = appName;
    }

    public abstract String configName();

    public abstract CONFIG joinTenantConfiguration(List<CONFIG_FILE> files);

    public abstract JavaType configFileJavaType(TypeFactory typeFactory);

    public ObjectMapper buildObjectMapper() {
        return new ObjectMapper(new YAMLFactory());
    }

    public void onUpdate(CONFIG configuration) {
        // to override
    }

    public String configFileExtension() {
        return "yml";
    }

    public List<String> filesPathAntPatterns() {
        String fileExtension = configFileExtension();
        String configName = configName();
        return List.of(
            "/config/tenants/{tenantName}/" + appName + "/" + configName + "." + fileExtension,
            "/config/tenants/{tenantName}/" + appName + "/" + configName + "/*." + fileExtension
        );
    }

    // to override
    public String getTenantVariableName() {
        return TENANT_NAME;
    }

    public String getTenantKey(String filePath, String pattern) {
        return pathMatcher.extractUriTemplateVariables(pattern, filePath).get(getTenantVariableName());
    }


    // by default protected, if need can be made public in child class
    protected CONFIG getConfiguration(String tenantKey) {
        return configurationsByTenant.get(tenantKey);
    }

    protected Map<String, CONFIG_FILE> getConfigurationFiles() {
        String tenantKey = tenantContextHolder.getTenantKey();
        return configurationsByTenantByFile.computeIfAbsent(tenantKey, k -> new ConcurrentHashMap<>());
    }

    public CONFIG getConfiguration() {
        return getConfiguration(tenantContextHolder.getTenantKey());
    }

    @Override
    public final void onInit(String configKey, String configValue) {
        onRefresh(configKey, configValue);
    }

    @Override
    @SneakyThrows
    public final void onRefresh(String updatedKey, String config) {
        try {
            String pattern = findPattern(updatedKey);
            String tenantKey = getTenantKey(updatedKey, pattern);
            Map<String, CONFIG_FILE> byFiles = configurationsByTenantByFile.computeIfAbsent(tenantKey, k -> new ConcurrentHashMap<>());

            if (StringUtils.isBlank(config)) {
                byFiles.remove(updatedKey);
                log.info("Configuration by key: {} was removed", updatedKey);
            } else {
                CONFIG_FILE configurationItem = mapper.readValue(config, configFileJavaType(mapper.getTypeFactory()));
                byFiles.put(updatedKey, configurationItem);
                log.info("Configuration by key: {} was updated", updatedKey);
            }

            CONFIG value = joinTenantConfiguration(List.copyOf(byFiles.values()));
            onUpdate(value);
            configurationsByTenant.put(tenantKey, value);
        } catch (Exception e) {
            log.error("Error update configuration by key: {}", updatedKey, e);
        }
    }

    private String findPattern(String updatedKey) {
        return filesPathAntPatterns().stream()
            .filter(pattern -> pathMatcher.match(pattern, updatedKey))
            .findFirst()
            // impossible
            .orElseThrow(() -> new IllegalArgumentException("Unsupported configuration key: " + updatedKey));
    }

    @Override
    public final boolean isListeningConfiguration(String updatedKey) {
        return filesPathAntPatterns().stream().anyMatch(pattern -> pathMatcher.match(pattern, updatedKey));
    }

}
