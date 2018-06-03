package com.icthh.xm.commons.config.client.service;

import static java.util.Collections.emptyMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing custom tenant tenant-config.yml across all micro-services. This service is designed for: <p>-
 * usage in LEP for saving shared configuration. <p>- other need for tenant configuration.
 */
@Slf4j
public class TenantConfigService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";

    public static final String DEFAULT_TENANT_CONFIG_PATTERN = "/config/tenants/{tenantName}/tenant-config.yml";

    private ConcurrentHashMap<String, Map<String, Object>> tenantConfig = new ConcurrentHashMap<>();

    private final XmConfigProperties xmConfigProperties;

    private final AntPathMatcher matcher = new AntPathMatcher();

    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private final TenantContextHolder tenantContextHolder;

    public TenantConfigService(XmConfigProperties xmConfigProperties,
                               TenantContextHolder tenantContextHolder) {
        this.xmConfigProperties = xmConfigProperties;
        this.tenantContextHolder = tenantContextHolder;
    }

    @IgnoreLogginAspect
    public Map<String, Object> getConfig() {
        return getTenantConfig();
    }

    private Map<String, Object> getTenantConfig() {
        return tenantConfig.getOrDefault(getTenantKeyValue(), emptyMap());
    }

    private String getTenantKeyValue() {
        return TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
    }

    private String getTenantConfigPattern() {
        String tenantConfigPattern = xmConfigProperties.getTenantConfigPattern();
        return StringUtils.isBlank(tenantConfigPattern) ? DEFAULT_TENANT_CONFIG_PATTERN : tenantConfigPattern;
    }

    private void processMap(Map<String, Object> map) {
        nullSafe(map).entrySet().forEach(e -> {
            Object value = e.getValue();
            if (value instanceof Map) {
                Map inner = Map.class.cast(value);
                e.setValue(Collections.unmodifiableMap(inner));
                processMap(inner);
            } else if (value instanceof List) {
                List inner = List.class.cast(value);
                e.setValue(Collections.unmodifiableList(inner));
                processList(inner);
            }
        });
    }

    private Map<String, Object> nullSafe(Map<String, Object> map) {
        return map == null ? emptyMap() : map;
    }

    private void processList(List<Object> list) {
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);

            if (item instanceof Map) {
                Map inner = Map.class.cast(item);
                list.set(i, Collections.unmodifiableMap(inner));
                processMap(inner);
            } else if (item instanceof List) {
                List inner = List.class.cast(item);
                list.set(i, Collections.unmodifiableList(inner));
                processList(inner);
            }
        }
    }

    @Override
    public void onRefresh(final String updatedKey, final String config) {

        String tenantConfigPattern = getTenantConfigPattern();
        try {
            String tenant = matcher.extractUriTemplateVariables(tenantConfigPattern, updatedKey).get(TENANT_NAME);
            if (org.apache.commons.lang3.StringUtils.isBlank(config)) {
                tenantConfig.remove(tenant);
                return;
            }
            Map<String, Object> configMap = mapper.readValue(config, Map.class);

            processMap(configMap);

            tenantConfig.put(tenant, Collections.unmodifiableMap(configMap));
            log.info("Tenant configuration was updated for tenant [{}] by key [{}]", tenant, updatedKey);
        } catch (Exception e) {
            log.error("Error read tenant configuration from path " + updatedKey, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(final String updatedKey) {
        String tenantConfigPattern = getTenantConfigPattern();
        return matcher.match(tenantConfigPattern, updatedKey);
    }

    @Override
    public void onInit(final String configKey, final String configValue) {
        if (isListeningConfiguration(configKey)) {
            onRefresh(configKey, configValue);
        }
    }

}
