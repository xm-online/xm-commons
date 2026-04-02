package com.icthh.xm.commons.config.client.service;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.MapperFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.config.domain.TenantAliasTree;
import java.io.IOException;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class TenantAliasServiceImpl implements TenantAliasService {

    private final ObjectMapper mapper = YAMLMapper.builder()
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build();

    // tenant list repository present to ensure that ms config is up and running
    public TenantAliasServiceImpl(CommonConfigRepository commonConfigRepository,
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
        } catch (JacksonException e) {
            log.error("Error parse tenant alias config", e);
        }
    }

}
