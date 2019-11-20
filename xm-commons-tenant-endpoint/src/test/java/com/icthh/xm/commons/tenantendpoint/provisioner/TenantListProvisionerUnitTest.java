package com.icthh.xm.commons.tenantendpoint.provisioner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantListProvisioner;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TenantListProvisionerUnitTest {

    private static final String TENANT_KEY = "testKey";
    private static final String TENANT_STATE = "testState";

    private TenantListProvisioner tenantListProvisioner;

    @Mock
    private TenantListRepository tenantListRepository;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        tenantListProvisioner = new TenantListProvisioner(tenantListRepository);
    }

    @Test
    public void testCreateTenant() {
        Tenant tenant = new Tenant().tenantKey(TENANT_KEY);
        tenantListProvisioner.createTenant(tenant);

        verify(tenantListRepository, times(1)).addTenant(eq(TENANT_KEY));
    }

    @Test
    public void testManageTenant() {
        tenantListProvisioner.manageTenant(TENANT_KEY, TENANT_STATE);

        verify(tenantListRepository, times(1)).updateTenant(TENANT_KEY, TENANT_STATE);
    }

    @Test
    public void testDeleteTenant() {
        tenantListProvisioner.deleteTenant(TENANT_KEY);

        verify(tenantListRepository, times(1)).deleteTenant(TENANT_KEY);
    }
}
