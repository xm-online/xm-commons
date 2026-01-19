package com.icthh.xm.commons.tenantendpoint.provisioner;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    PropertySourcesPlaceholderConfigurer.class,
    TenantContextConfiguration.class,
    XmAuthenticationContextConfiguration.class,
    TenantAbilityCheckerProvisioner.class
})
class TenantAbilityCheckerProvisionerIntTest {

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    TenantAbilityCheckerProvisioner tenantAbilityCheckerProvisioner;

    @BeforeEach
    void init() {
        TenantContextUtils.setTenant(tenantContextHolder, "XM");
    }

    @AfterEach
    public void after() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void createTenant() {
        tenantAbilityCheckerProvisioner.createTenant(new Tenant().tenantKey("NEWTENANT"));
    }

    @Test
    public void createTenantNotAllowed() {
        TenantContextUtils.setTenant(tenantContextHolder, "TENANTKEY");

        assertThrows(BusinessException.class,
            () -> tenantAbilityCheckerProvisioner.createTenant(new Tenant().tenantKey("NEWTENANT")),
            "Only [XM] tenants allowed to create new tenant"
        );
    }

    @Test
    public void manageTenant() {
        tenantAbilityCheckerProvisioner.manageTenant("NEWTENANT", "ACTIVE");
    }

    @Test
    public void manageTenantNotAllowed() {
        TenantContextUtils.setTenant(tenantContextHolder, "TENANTKEY");

        assertThrows(BusinessException.class,
            () -> tenantAbilityCheckerProvisioner.manageTenant("NEWTENANT", "ACTIVE"),
            "Only [XM] tenants allowed to manage tenant"
        );
    }

    @Test
    public void deleteTenant() {
        tenantAbilityCheckerProvisioner.deleteTenant("NEWTENANT");
    }

    @Test
    public void deleteTenantNotAllowed() {
        TenantContextUtils.setTenant(tenantContextHolder, "TENANTKEY");

        assertThrows(BusinessException.class,
            () -> tenantAbilityCheckerProvisioner.deleteTenant("NEWTENANT"),
            "Only [XM] tenants allowed to delete tenant"
        );
    }
}
