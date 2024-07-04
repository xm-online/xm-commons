package com.icthh.xm.commons.flow.spec.resource;

import com.icthh.xm.commons.config.client.api.refreshable.MapRefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TenantResourceTypeService extends MapRefreshableConfiguration<TenantResourceType, TenantResourceTypesConfig> {

    public TenantResourceTypeService(@Value("${spring.application.name}") String appName,
                                     TenantContextHolder tenantContextHolder) {
        super(appName, tenantContextHolder);
    }

    @Override
    protected List<TenantResourceType> toConfigItems(TenantResourceTypesConfig config) {
        return config.getTenantResourceTypes();
    }

    @Override
    public Class<TenantResourceTypesConfig> configFileClass() {
        return TenantResourceTypesConfig.class;
    }

    @Override
    public String configName() {
        return "resource-types";
    }

    public List<TenantResourceType> resourceTypes() {
        return List.copyOf(getConfiguration().values());
    }

    public TenantResourceType getResource(String resourceType) {
        return getConfiguration().get(resourceType);
    }
}
