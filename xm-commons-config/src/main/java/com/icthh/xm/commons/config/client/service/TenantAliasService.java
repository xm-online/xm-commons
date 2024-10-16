package com.icthh.xm.commons.config.client.service;

import com.icthh.xm.commons.config.domain.TenantAliasTree;

public interface TenantAliasService {

    String TENANT_ALIAS_CONFIG = "/config/tenants/tenant-aliases.yml";

    TenantAliasTree getTenantAliasTree();
    void onRefresh(String config);

}
