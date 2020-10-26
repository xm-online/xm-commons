package com.icthh.xm.commons.config.client.service;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SecurityTenantConfigService extends TenantConfigService {

    private final Map<String, SecurityTenantConfig> configs = new ConcurrentHashMap<>();
    private final TenantContextHolder tenantContextHolder;

    public SecurityTenantConfigService(XmConfigProperties xmConfigProperties,
        TenantContextHolder tenantContextHolder) {
        super(xmConfigProperties, tenantContextHolder);
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        try {
            super.onRefresh(updatedKey, config);
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
            config = skipNullFields(config, objectMapper);
            SecurityTenantConfig value = objectMapper.readValue(config, SecurityTenantConfig.class);
            configs.put(getTenantKey(updatedKey), value);

            log.info("config )))>>>>> {}", config);

            log.info("..........>>>>>>>>>> {}", configs);
        } catch (Exception e) {
            log.error("Error read tenant configuration from path [{}]", updatedKey, e);
        }

        super.onRefresh(updatedKey, config);
    }

    private String skipNullFields(String config, ObjectMapper objectMapper) throws java.io.IOException {
        objectMapper.setSerializationInclusion(NON_NULL);
        return objectMapper.writeValueAsString(objectMapper.readValue(config, Map.class));
    }

    public SecurityTenantConfig getSecurityTenantConfig() {
        String tenantKey = tenantContextHolder.getTenantKey();
        configs.computeIfAbsent(tenantKey, (key) -> new SecurityTenantConfig());
        return configs.get(tenantKey);
    }

    public SecurityTenantConfig getSecurityTenantConfig(String tenantKey) {
        return configs.computeIfAbsent(tenantKey, (key) -> new SecurityTenantConfig());
    }

    @Data
    public static class SecurityTenantConfig {

        private PermissionConfig permissionConfig = new PermissionConfig();

        String bbb;
        @Data
        public static class PermissionConfig {
            private Boolean separatePermissionFile = false;
            String aaa;
        }
    }
}
