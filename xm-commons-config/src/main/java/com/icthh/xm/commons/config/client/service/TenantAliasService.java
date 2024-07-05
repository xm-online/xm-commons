package com.icthh.xm.commons.config.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.config.domain.TenantAliasTree;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@Slf4j
public class TenantAliasService {

    public static final String TENANT_ALIAS_CONFIG = "/config/tenants/tenant-aliases.yml";

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    // tenant list repository present to ensure that ms config is up and running
    public TenantAliasService(CommonConfigRepository commonConfigRepository,
                              TenantListRepository tenantListRepository) {
        requireNonNull(tenantListRepository, "tenantListRepository can't be null");

        Map<String, Configuration> configMap = commonConfigRepository.getConfig(null, singletonList(TENANT_ALIAS_CONFIG));
        configMap = firstNonNull(configMap, emptyMap());
        Configuration configuration = configMap.get(TENANT_ALIAS_CONFIG);
        if (configuration != null && StringUtils.isNotBlank(configuration.getContent())) {
            onRefresh(configuration.getContent());
        } else {
            log.info("Tenant aliases not configured.");
        }
    }

    @Getter
    private volatile TenantAliasTree tenantAliasTree = new TenantAliasTree();

    public void onRefresh(String config) {
        try {
            TenantAliasTree tenantAliasTree = mapper.readValue(config, TenantAliasTree.class);
            tenantAliasTree.init();
            // safe publication
            this.tenantAliasTree = tenantAliasTree;
            log.info("Tenant aliases inited");
            log.trace("Tenant aliases inited: {}", config);
        } catch (IOException e) {
            log.error("Error parse tenant alias config", e);
        }
    }

}
