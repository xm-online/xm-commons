package com.icthh.xm.commons.config.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.domain.TenantAliasTree;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class TenantAliasService implements RefreshableConfiguration {

    public static final String TENANT_ALIAS_CONFIG = "/config/tenants/tenant-aliases.yml";
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Getter
    private volatile TenantAliasTree tenantAliasTree = new TenantAliasTree();

    @Override
    public void onRefresh(String updatedKey, String config) {
        try {
            TenantAliasTree tenantAliasTree = mapper.readValue(config, TenantAliasTree.class);
            tenantAliasTree.init();
            // safe publication
            this.tenantAliasTree = tenantAliasTree;
        } catch (IOException e) {
            log.error("Error parse tenant alias config", e);
        }
    }

    @Override
    public boolean isListeningConfiguration(String path) {
        return TENANT_ALIAS_CONFIG.equals(path);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        onRefresh(configKey, configValue);
    }

}
