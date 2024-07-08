package com.icthh.xm.commons.flow.spec.resource;

import com.icthh.xm.commons.config.client.api.refreshable.MapRefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TenantResourceTypeService extends MapRefreshableConfiguration<TenantResourceType, TenantResourceTypesSpec> {

    public TenantResourceTypeService(@Value("${spring.application.name}") String appName,
                                     TenantContextHolder tenantContextHolder) {
        super(appName, tenantContextHolder);
    }

    @Override
    protected List<TenantResourceType> toConfigItems(TenantResourceTypesSpec config) {
        return config.getTenantResourceTypes();
    }

    @Override
    public Class<TenantResourceTypesSpec> configFileClass() {
        return TenantResourceTypesSpec.class;
    }

    @Override
    public String configName() {
        return "resource-types";
    }

    @Override
    public String folder() {
        return "/flow";
    }

    public List<TenantResourceType> resourceTypes() {
        return List.copyOf(getConfiguration().values());
    }

    public TenantResourceType getResource(String resourceType) {
        return getConfiguration().get(resourceType);
    }
}
