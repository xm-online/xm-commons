package com.icthh.xm.commons.logging.configurable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.logging.config.LoggingConfig;
import com.icthh.xm.commons.logging.config.LoggingConfig.LepLogConfiguration;
import com.icthh.xm.commons.logging.config.LoggingConfig.LogConfiguration;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;

@Slf4j
@Component
@Primary
public class LoggingRefreshableConfiguration implements RefreshableConfiguration, LoggingConfigService {

    private final ConcurrentHashMap<String, Map<String, LogConfiguration>> serviceLoggingConfig = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, LogConfiguration>> apiLoggingConfig = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, LepLogConfiguration>> lepLoggingConfig = new ConcurrentHashMap<>();

    private final AntPathMatcher matcher = new AntPathMatcher();
    private ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());

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
            LoggingConfig spec = ymlMapper.readValue(config, LoggingConfig.class);

            if (spec == null) {
                this.apiLoggingConfig.remove(tenant);
                this.serviceLoggingConfig.remove(tenant);
                this.lepLoggingConfig.remove(tenant);
                return;
            }

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
        return getLogConfiguration(serviceLoggingConfig.get(getRequiredTenantKeyValue(this.tenantContextHolder)),
            packageName,
            className,
            methodName);

    }

    @Override
    public LogConfiguration getApiLoggingConfig(String packageName, String className, String methodName) {
        return getLogConfiguration(apiLoggingConfig.get(getRequiredTenantKeyValue(this.tenantContextHolder)),
            packageName,
            className,
            methodName);
    }

    @Override
    public LepLogConfiguration getLepLoggingConfig(String fileName) {
        String tenantKey = getRequiredTenantKeyValue(this.tenantContextHolder);


        Map<String, LepLogConfiguration> logConfiguration = lepLoggingConfig.get(tenantKey);
        if (MapUtils.isEmpty(logConfiguration)) {
            return null;
        }
        if (logConfiguration.containsKey(fileName)) {
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

        if (logConfiguration.containsKey(className)) {
            return logConfiguration.get(className);
        }
        if (logConfiguration.containsKey(className + ":" + methodName)) {
            return logConfiguration.get(className + ":" + methodName);
        }
        if (logConfiguration.containsKey(packageName + ":" + className + ":" + methodName)) {
            return logConfiguration.get(packageName + ":" + className + ":" + methodName);
        }

        return null;

    }

}
