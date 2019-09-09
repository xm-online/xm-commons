package com.icthh.xm.commons.tenantendpoint.provisioner;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.gen.model.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantListProvisioner implements TenantProvisioner {

    private final TenantListRepository tenantListRepository;

    @Override
    public void createTenant(Tenant tenant) {
        tenantListRepository.addTenant(tenant.getTenantKey());
    }

    @Override
    public void manageTenant(String tenantKey, String state) {
        tenantListRepository.updateTenant(tenantKey, state);
    }

    @Override
    public void deleteTenant(String tenantKey) {
        tenantListRepository.deleteTenant(tenantKey);
    }
}
