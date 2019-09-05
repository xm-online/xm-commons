package com.icthh.xm.commons.tenantendpoint;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.icthh.xm.commons.gen.model.Tenant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

public class TenantManagerUnitTest {

    private static final String TENANT_KEY = "testKey";
    private static final String TENANT_STATE = "testState";

    private TenantManager tenantManager;

    @Mock
    private TenantProvisioner service;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        tenantManager = new TenantManager(Collections.singletonList(service));
    }

    @Test
    public void testCreateTenant() {
        Tenant tenant = new Tenant().tenantKey(TENANT_KEY);
        tenantManager.createTenant(tenant);

        verify(service, times(1)).createTenant(eq(tenant));
    }

    @Test
    public void testManageTenant() {
        tenantManager.manageTenant(TENANT_KEY, TENANT_STATE);

        verify(service, times(1)).manageTenant(TENANT_KEY, TENANT_STATE);
    }

    @Test
    public void testDeleteTenant() {
        tenantManager.deleteTenant(TENANT_KEY);

        verify(service, times(1)).deleteTenant(TENANT_KEY);
    }
}
