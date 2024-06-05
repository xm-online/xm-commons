package com.icthh.xm.commons.config.client.service;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.icthh.xm.commons.config.client.service.TenantAliasService.TENANT_ALIAS_CONFIG;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@Slf4j
@RequiredArgsConstructor
public class TenantAliasServiceConfiguration implements RefreshableConfiguration {

    private final TenantAliasService tenantAliasService;

    @Override
    public void onRefresh(String updatedKey, String config) {
        tenantAliasService.onRefresh(config);
    }

    @Override
    public boolean isListeningConfiguration(String path) {
        return TENANT_ALIAS_CONFIG.equals(path);
    }

}
