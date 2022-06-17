package com.icthh.xm.commons.tenantendpoint.provisioner;

import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestPropertySource;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
    TenantContextConfiguration.class,
    XmAuthenticationContextConfiguration.class
})
@TestPropertySource(properties = "application.tenant-with-creation-access-list:['XM', 'MANAGER']")
public class TenantAbilityCheckerProvisionerIntTest {

    @Test
    public void createTenant() {
    }

    @Test
    public void manageTenant() {
    }

    @Test
    public void deleteTenant() {
    }
}
