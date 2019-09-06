package com.icthh.xm.commons.tenantendpoint;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.gen.model.Tenant;
import lombok.Builder;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.List;

@Service
@Builder
@Slf4j
public class TenantConfigProvisioner implements TenantProvisioner {

    @Singular
    private List<Configuration> configurations;
    private final TenantConfigRepository tenantConfigRepository;

    public static String prependTenantPath(String path) {
        return Paths.get(TenantConfigRepository.PATH_CONFIG_TENANT, path).toString();
    }

    @Override
    public void createTenant(final Tenant tenant) {
        String tenantKey = tenant.getTenantKey();
        tenantConfigRepository.createConfigsFullPath(tenantKey, configurations);
    }

    @Override
    public void manageTenant(final String tenantKey, final String state) {
        log.info("Nothing to do with ms-config during manage tenant: {}, state = {}", tenantKey, state);
    }

    @Override
    public void deleteTenant(final String tenantKey) {
        tenantConfigRepository.deleteConfigFullPath(tenantKey, TenantConfigRepository.PATH_API_CONFIG_TENANT);
    }
}
