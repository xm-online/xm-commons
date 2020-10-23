package com.icthh.xm.commons.tenantendpoint;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantProvisioner;
import lombok.Builder;
import lombok.Singular;

import java.util.List;
import java.util.function.Consumer;
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

    @Builder.Default
    private Consumer<? super Exception> exceptionHandler = defaultExceptionHandler();

    /**
     * Executed createTenant() on all {@link TenantProvisioner} services.
     *
     * @param tenant - tenant model
     */
    public void createTenant(Tenant tenant) {
        withExceptionHandler(
            () -> services.forEach(tenantService -> tenantService.createTenant(tenant)));
    }

    /**
     * Executed manageTenant() on all {@link TenantProvisioner} services.
     *
     * @param tenantKey - tenant key
     * @param state     - tenant state
     */
    public void manageTenant(String tenantKey, String state) {
        withExceptionHandler(
            () -> services.forEach(tenantService -> tenantService.manageTenant(tenantKey, state)));
    }

    /**
     * Executed deleteTenant() on all {@link TenantProvisioner} services.
     *
     * @param tenantKey - tenant key
     */
    public void deleteTenant(String tenantKey) {
        withExceptionHandler(
            () -> services.forEach(tenantService -> tenantService.deleteTenant(tenantKey)));
    }

    private void withExceptionHandler(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            exceptionHandler.accept(e);
        }

    }

    private static <E extends Exception> Consumer<E> defaultExceptionHandler() {
        return e -> {
            if (e instanceof BusinessException) {
                throw (BusinessException)e;
            }
            throw new BusinessException(e.getMessage());
        };
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
