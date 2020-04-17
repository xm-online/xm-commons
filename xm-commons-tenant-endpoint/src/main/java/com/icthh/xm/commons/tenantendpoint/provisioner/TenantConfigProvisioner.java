package com.icthh.xm.commons.tenantendpoint.provisioner;

import static com.icthh.xm.commons.config.client.repository.TenantConfigRepository.PATH_API_CONFIG_TENANT;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.gen.model.Tenant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Builder
@Slf4j
@AllArgsConstructor
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class TenantConfigProvisioner implements TenantProvisioner {

    @Singular
    private List<Configuration> configurations;
    private final TenantConfigRepository tenantConfigRepository;

    public static String prependTenantPath(String path) {
        return FilenameUtils.separatorsToUnix(Paths.get(TenantConfigRepository.PATH_CONFIG_TENANT, path).toString());
    }

    @Override
    public void createTenant(final Tenant tenant) {
        withConfigurations(tenant.getTenantKey(), () -> {
            String tenantKey = tenant.getTenantKey();
            tenantConfigRepository.createConfigsFullPath(tenantKey, configurations);
        });
    }

    @Override
    public void manageTenant(final String tenantKey, final String state) {
        log.info("Nothing to do with ms-config during manage tenant: {}, state = {}", tenantKey, state);
    }

    @Override
    public void deleteTenant(final String tenantKey) {
        withConfigurations(tenantKey,
                           () -> tenantConfigRepository.deleteConfigFullPath(tenantKey, PATH_API_CONFIG_TENANT));
    }

    @Override
    public String toString() {
        return "TenantConfigProvisioner {" +
               " configurations = " + configurations.stream()
                                                    .map(Configuration::getPath)
                                                    .collect(Collectors.toList()) +
               '}';
    }

    private void withConfigurations(String tenant, Runnable runnable) {
        if (configurations != null && !configurations.isEmpty()) {
            runnable.run();
        } else {
            log.warn("Skip ms-config provisioning as configuration list was not added. tenant: {}", tenant);
        }
    }

}
