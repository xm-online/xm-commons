package com.icthh.xm.commons.tenantendpoint.configurer;

import com.icthh.xm.commons.tenantendpoint.TenantManager;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantProvisioner;

import java.util.List;

public class TenantCreationConfigurer implements TenantManagerConfigurer {

    private final List<TenantProvisioner> tenantProvisioners ;

    public TenantCreationConfigurer(List<TenantProvisioner> tenantProvisioners) {
        this.tenantProvisioners = tenantProvisioners;
    }

    @Override
    public void configure(TenantManager.TenantManagerBuilder builder) {
        builder.services(tenantProvisioners);
    }
}
