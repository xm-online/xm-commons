package com.icthh.xm.commons.config.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.domain.TenantAliasTree;
import java.io.IOException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantAliasPreCompileServiceImpl implements TenantAliasService {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Getter
    private volatile TenantAliasTree tenantAliasTree = new TenantAliasTree();

    // Pre compile tenant aliases should not connect to ms config
    public TenantAliasPreCompileServiceImpl() {};

    public void onRefresh(String config) {
        try {
            TenantAliasTree tenantAliasTree = mapper.readValue(config, TenantAliasTree.class);
            tenantAliasTree.init();
            // safe publication
            this.tenantAliasTree = tenantAliasTree;
            log.info("Tenant aliases inited");
        } catch (IOException e) {
            log.error("Error parse tenant alias config", e);
        }
    }

}
