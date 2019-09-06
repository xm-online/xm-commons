package com.icthh.xm.commons.tenantendpoint;

import com.icthh.xm.commons.gen.model.Tenant;
import lombok.Builder;
import lombok.Singular;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for management tenant creation, update and deletion.
 *
 * Contains list of actions - implementing interface {@link TenantProvisioner} and executed one by one.
 *
 * This service should be used in {@link com.icthh.xm.commons.gen.api.TenantsApiDelegate} endpoint to manage tenants.
 *
 * Exception in one provisioner interrupts execution and exit.
 */
@Builder
public class TenantManager {

    @Singular
    private List<TenantProvisioner> services;

    /**
     * Executed createTentnt() on all {@link TenantProvisioner} services.
     *
     * @param tenant - tenant model
     */
    public void createTenant(Tenant tenant) {
        services.forEach(tenantService -> tenantService.createTenant(tenant));
    }

    /**
     * Executed manageTenant() on all {@link TenantProvisioner} services.
     *
     * @param tenantKey - tenant key
     * @param state     - tenant state
     */
    public void manageTenant(String tenantKey, String state) {
        services.forEach(tenantService -> tenantService.manageTenant(tenantKey, state));
    }

    /**
     * Executed deleteenant() on all {@link TenantProvisioner} services.
     *
     * @param tenantKey - tenant key
     */
    public void deleteTenant(String tenantKey) {
        services.forEach(tenantService -> tenantService.deleteTenant(tenantKey));
    }

    @Override
    public String toString() {
        return "TenantManager {" +
               " services= " + services.stream()
                                       .map(srv -> srv.getClass().getSimpleName())
                                       .collect(Collectors.toList()) +
               '}';
    }
}
