package com.icthh.xm.commons.logging.configurable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.logging.config.LoggingConfig;
import com.icthh.xm.commons.logging.config.LoggingConfig.LepLogConfiguration;
import com.icthh.xm.commons.logging.config.LoggingConfig.LogConfiguration;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getTenantKey;

@Slf4j
@Component
@Primary
public class LoggingRefreshableConfiguration implements RefreshableConfiguration, LoggingConfigService {

    private final Map<String, Map<String, LogConfiguration>> serviceLoggingConfig = new ConcurrentHashMap<>();
    private final Map<String, Map<String, LogConfiguration>> apiLoggingConfig = new ConcurrentHashMap<>();
    private final Map<String, Map<String, LepLogConfiguration>> lepLoggingConfig = new ConcurrentHashMap<>();

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());

    private final TenantContextHolder tenantContextHolder;
    private final String mappingPath;

    public LoggingRefreshableConfiguration(TenantContextHolder tenantContextHolder,
                                           @Value("${spring.application.name}") String appName) {
        this.tenantContextHolder = tenantContextHolder;
        this.mappingPath = "/config/tenants/{tenantName}/" + appName + "/logging.yml";
    }

    @Override
    public void onRefresh(final String updatedKey, final String config) {
        try {
            String tenant = this.matcher.extractUriTemplateVariables(mappingPath, updatedKey).get("tenantName");
            if (StringUtils.isBlank(config)) {
                this.apiLoggingConfig.remove(tenant);
                this.serviceLoggingConfig.remove(tenant);
                this.lepLoggingConfig.remove(tenant);
                return;
            }

            LoggingConfig spec = ymlMapper.readValue(config, LoggingConfig.class);
            this.serviceLoggingConfig.put(tenant, spec.buildServiceLoggingConfigs());
            this.apiLoggingConfig.put(tenant, spec.buildApiLoggingConfigs());
            this.lepLoggingConfig.put(tenant, spec.buildLepLoggingConfigs(tenant));

            log.info("Tenant configuration was updated for tenant [{}] by key [{}]", tenant, updatedKey);
        } catch (Exception e) {
            log.error("Error read tenant configuration from path " + updatedKey, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(final String updatedKey) {
        return this.matcher.match(mappingPath, updatedKey);
    }

    @Override
    public void onInit(final String configKey, final String configValue) {
        if (this.isListeningConfiguration(configKey)) {
            this.onRefresh(configKey, configValue);
        }
    }

    @Override
    public LogConfiguration getServiceLoggingConfig(String packageName,
                                                              String className,
                                                              String methodName) {

        Optional<TenantKey> tenantKey = getTenantKey(this.tenantContextHolder);
        if (tenantKey.isPresent()) {
            String key = tenantKey.get().getValue();
            return getLogConfiguration(serviceLoggingConfig.get(key), packageName, className, methodName);
        }
        return null;
    }

    @Override
    public LogConfiguration getApiLoggingConfig(String packageName, String className, String methodName) {

        Optional<TenantKey> tenantKey = getTenantKey(this.tenantContextHolder);
        if (tenantKey.isPresent()) {
            String key = tenantKey.get().getValue();
            return getLogConfiguration(apiLoggingConfig.get(key), packageName, className, methodName);
        }
        return null;
    }

    @Override
    public LepLogConfiguration getLepLoggingConfig(String fileName) {
        if (fileName == null) {
            return null;
        }

        Optional<TenantKey> tenantKey = getTenantKey(this.tenantContextHolder);
        if (tenantKey.isPresent()) {
            String key = tenantKey.get().getValue();
            Map<String, LepLogConfiguration> logConfiguration = lepLoggingConfig.get(key);
            if (MapUtils.isEmpty(logConfiguration)) {
                return null;
            }
            return logConfiguration.get(fileName);
        }
        return null;
    }

    private LogConfiguration getLogConfiguration(Map<String, LogConfiguration> logConfiguration,
                                                 String packageName,
                                                 String className,
                                                 String methodName) {
        if (MapUtils.isEmpty(logConfiguration)) {
            return null;
        }

        LogConfiguration configuration = logConfiguration.get(packageName + ":" + className + ":" + methodName);

        if (configuration == null) {
            configuration = logConfiguration.get(className + ":" + methodName);
        }

        if (configuration == null) {
            configuration = logConfiguration.get(className);
        }
        return configuration;
    }

}
