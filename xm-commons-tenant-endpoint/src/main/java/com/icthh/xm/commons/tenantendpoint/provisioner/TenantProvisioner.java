package com.icthh.xm.commons.tenantendpoint.provisioner;

import com.icthh.xm.commons.gen.model.Tenant;

/**
 * Interface for tenant management actions on particular resource such as Database, xm-config, Elasticsearch etc.
 *
 * <p>
 * Known implementations:
 *      <li> {@link TenantAbilityCheckerProvisioner}
 *      <li> {@link TenantListProvisioner}
 *      <li> {@link TenantConfigProvisioner}
 *      <li> com.icthh.xm.commons.migration.db.tenant.provisioner.TenantDatabaseProvisioner
 * </p>
 */
public interface TenantProvisioner {

    /**
     * Creates new tenant on specific resource.
     *
     * @param tenant tenant model.
     */
    void createTenant(Tenant tenant);

    /**
     * Manages tenant state on specific resource.
     *
     * @param tenantKey tenant key
     * @param state     target tenant state.
     */
    void manageTenant(String tenantKey, String state);

    /**
     * Deletes tenant on specific resource.
     *
     * @param tenantKey tenant key
     */
    void deleteTenant(String tenantKey);

}
