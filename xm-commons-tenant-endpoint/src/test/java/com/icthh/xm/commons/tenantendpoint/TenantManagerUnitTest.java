package com.icthh.xm.commons.tenantendpoint;

import static com.icthh.xm.commons.tenantendpoint.TenantManager.builder;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantProvisioner;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class TenantManagerUnitTest {

    private static final String TENANT_KEY = "testKey";
    private static final String TENANT_STATE = "testState";

    private TenantManager tenantManager;

    @Mock
    private TenantProvisioner service1;

    @Mock
    private TenantProvisioner service2;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        tenantManager = builder().service(service1)
                                 .service(service2).build();
    }

    @Test
    public void testCreateTenant() {
        Tenant tenant = new Tenant().tenantKey(TENANT_KEY);
        tenantManager.createTenant(tenant);

        InOrder inOrder = Mockito.inOrder(service1, service2);
        inOrder.verify(service1, times(1)).createTenant(eq(tenant));
        inOrder.verify(service2, times(1)).createTenant(eq(tenant));

    }

    @Test
    public void testCreateTenantFailOnFirstError() {
        doThrow(new RuntimeException("Bang!")).when(service1).createTenant(any());

        Tenant tenant = new Tenant().tenantKey(TENANT_KEY);
        runWithExceptionExpected(() -> tenantManager.createTenant(tenant), RuntimeException.class, "Bang!");

        verify(service1, times(1)).createTenant(eq(tenant));
        verify(service2, never()).createTenant(eq(tenant));
    }

    @Test
    public void testManageTenant() {
        tenantManager.manageTenant(TENANT_KEY, TENANT_STATE);
        InOrder inOrder = Mockito.inOrder(service1, service2);
        inOrder.verify(service1, times(1)).manageTenant(TENANT_KEY, TENANT_STATE);
        inOrder.verify(service2, times(1)).manageTenant(TENANT_KEY, TENANT_STATE);
    }

    @Test
    public void testManageTenantFailOnFirstError() {
        doThrow(new RuntimeException("Bang!")).when(service1).manageTenant(any(), any());

        runWithExceptionExpected(() -> tenantManager.manageTenant(TENANT_KEY, TENANT_STATE),
                                 RuntimeException.class, "Bang!");

        verify(service1, times(1)).manageTenant(TENANT_KEY, TENANT_STATE);
        verify(service2, never()).manageTenant(TENANT_KEY, TENANT_STATE);
    }

    @Test
    public void testDeleteTenant() {
        tenantManager.deleteTenant(TENANT_KEY);

        InOrder inOrder = Mockito.inOrder(service1, service2);
        inOrder.verify(service1, times(1)).deleteTenant(TENANT_KEY);
        inOrder.verify(service2, times(1)).deleteTenant(TENANT_KEY);

    }

    @Test
    public void testDeleteTenantOnFirstError() {
        doThrow(new RuntimeException("Bang!")).when(service1).deleteTenant(any());

        runWithExceptionExpected(() -> tenantManager.deleteTenant(TENANT_KEY),
                                 RuntimeException.class, "Bang!");

        verify(service1, times(1)).deleteTenant(TENANT_KEY);
        verify(service2, never()).deleteTenant(TENANT_KEY);
    }

    private void runWithExceptionExpected(Runnable r, Class<? extends Exception> type, String message) {

        try {
            r.run();
            fail("Expected exception: " + type + " with message: " + message);
        } catch (Exception e) {
            if (!e.getClass().equals(type)) {
                fail("Expected exception.class  : " + type + "\n\t\t\t\tactual was: " + e.getClass());
            }
            if (!String.valueOf(e.getMessage()).equals(message)) {
                fail("Expected exception.message: " + message + "\n\t\t\t\tactual was: " + e.getMessage());
            }
        }
    }

}
