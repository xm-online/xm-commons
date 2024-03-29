package com.icthh.xm.commons.tenantendpoint.provisioner;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
public class TenantAbilityCheckerProvisioner implements TenantProvisioner {

    private final TenantContextHolder tenantContextHolder;

    private final Set<String> allowedTenants;

    public TenantAbilityCheckerProvisioner(
        final TenantContextHolder tenantContextHolder,
        @Value("${application.tenant-with-creation-access-list:#{T(java.util.Set).of('XM')}}")
        Set<String> allowedTenants
    ) {
        this.tenantContextHolder = tenantContextHolder;
        this.allowedTenants = Collections.unmodifiableSet(allowedTenants);
    }

    @Override
    public void createTenant(final Tenant tenant) {
        assertCanManageTenant("create new");
    }

    @Override
    public void manageTenant(final String tenantKey, final String state) {
        assertCanManageTenant("manage");
    }

    @Override
    public void deleteTenant(final String tenantKey) {
        assertCanManageTenant("delete");
    }

    protected void assertCanManageTenant(String action) {
        if (!allowedTenants.contains(TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder))) {
            throw new BusinessException(
                "Only " + allowedTenants + String.format(" tenants allowed to %s tenant", action));
        }
    }

}
