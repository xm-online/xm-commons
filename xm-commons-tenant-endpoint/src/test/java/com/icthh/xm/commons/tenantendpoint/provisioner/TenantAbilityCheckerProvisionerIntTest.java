package com.icthh.xm.commons.tenantendpoint.provisioner;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
    PropertySourcesPlaceholderConfigurer.class,
    TenantContextConfiguration.class,
    XmAuthenticationContextConfiguration.class,
    TenantAbilityCheckerProvisioner.class
})
public class TenantAbilityCheckerProvisionerIntTest {

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    TenantAbilityCheckerProvisioner tenantAbilityCheckerProvisioner;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void init() {
        TenantContextUtils.setTenant(tenantContextHolder, new TenantKey("XM"));
    }

    @Test
    public void createTenant() {
        tenantAbilityCheckerProvisioner.createTenant(new Tenant().tenantKey("NEWTENANT"));
    }

    @Test
    public void createTenantNotAllowed() {
        TenantContextUtils.setTenant(tenantContextHolder, "TENANTKEY");

        exception.expect(BusinessException.class);
        exception.expectMessage("Only [XM] tenants allowed to create new tenant");

        tenantAbilityCheckerProvisioner.createTenant(new Tenant().tenantKey("NEWTENANT"));
        System.out.println("tenantContextHolder: " + tenantContextHolder.getTenantKey());
        verify(tenantContextHolder).getTenantKey();

    }

    @Test
    public void manageTenant() {
        tenantAbilityCheckerProvisioner.manageTenant("NEWTENANT", "ACTIVE");
    }

    @Test
    public void manageTenantNotAllowed() {
        TenantContextUtils.setTenant(tenantContextHolder, "TENANTKEY");

        exception.expect(BusinessException.class);
        exception.expectMessage("Only [XM] tenants allowed to manage tenant");

        tenantAbilityCheckerProvisioner.manageTenant("NEWTENANT", "ACTIVE");

    }

    @Test
    public void deleteTenant() {
        tenantAbilityCheckerProvisioner.deleteTenant("NEWTENANT");
    }

    @Test
    public void deleteTenantNotAllowed() {
        TenantContextUtils.setTenant(tenantContextHolder, "TENANTKEY");

        exception.expect(BusinessException.class);
        exception.expectMessage("Only [XM] tenants allowed to delete tenant");

        tenantAbilityCheckerProvisioner.deleteTenant("NEWTENANT");
    }
}
