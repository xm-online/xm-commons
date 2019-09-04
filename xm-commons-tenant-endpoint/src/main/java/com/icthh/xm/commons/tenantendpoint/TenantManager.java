package com.icthh.xm.commons.tenantendpoint;

import com.icthh.xm.commons.gen.model.Tenant;
import lombok.Builder;
import lombok.Singular;
import org.springframework.stereotype.Service;

import java.util.List;

@Builder
@Service
public class TenantManager {

    @Singular
    private List<TenantProvisioner> services;

    void createTenant(Tenant tenant) {
        services.forEach(tenantService -> tenantService.createTenant(tenant));
    }

    void manageTenant(String tenantKey, String state) {
        services.forEach(tenantService -> tenantService.manageTenant(tenantKey, state));
    }

    void deleteTenant(String tenantKey) {
        services.forEach(tenantService -> tenantService.deleteTenant(tenantKey));
    }
}
