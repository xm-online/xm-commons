package com.icthh.xm.commons.tenantendpoint.provisioner;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TenantAbilityCheckerProvisionerUnitTest {

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private TenantContext tenantContext;

    private TenantAbilityCheckerProvisioner provisioner;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        provisioner = new TenantAbilityCheckerProvisioner(tenantContextHolder, Set.of("XM"));
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
    }

    @Test
    public void createTenant() {

        when(tenantContext.getTenantKey()).thenReturn(Optional.of(new TenantKey("XM")));
        provisioner.createTenant(new Tenant().tenantKey("NEWTENANT"));
        verify(tenantContext).getTenantKey();

    }

    @Test
    public void createTenantNotAllowed() {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(new TenantKey("NONXM")));

        assertThrows(BusinessException.class,
            () -> provisioner.createTenant(new Tenant().tenantKey("NEWTENANT")),
            "Only [XM] tenants allowed to delete tenant"
        );
        verify(tenantContext).getTenantKey();

    }

    @Test
    public void customizedTenantNotAllowed() {
        Set<String> allowedTenants = new HashSet<>();
        allowedTenants.add("POWER");
        allowedTenants.add("ADMIN");

        provisioner = new TenantAbilityCheckerProvisioner(tenantContextHolder, allowedTenants);

        when(tenantContext.getTenantKey()).thenReturn(Optional.of(new TenantKey("POWER")));
        provisioner.createTenant(new Tenant().tenantKey("NEWTENANT"));
        verify(tenantContext, times(1)).getTenantKey();

        when(tenantContext.getTenantKey()).thenReturn(Optional.of(new TenantKey("ADMIN")));
        provisioner.createTenant(new Tenant().tenantKey("NEWTENANT"));
        verify(tenantContext, times(2)).getTenantKey();

        when(tenantContext.getTenantKey()).thenReturn(Optional.of(new TenantKey("XM")));

        assertThrows(BusinessException.class,
            () -> provisioner.createTenant(new Tenant().tenantKey("NEWTENANT")),
            "Only [POWER, ADMIN] tenants allowed to create new tenant"
        );
        verify(tenantContext, times(3)).getTenantKey();
    }

    @Test
    public void manageTenant() {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(new TenantKey("XM")));
        provisioner.manageTenant("NEWTENANT", "ACTIVE");
        verify(tenantContext).getTenantKey();
    }

    @Test
    public void manageTenantNotAllowed() {

        when(tenantContext.getTenantKey()).thenReturn(Optional.of(new TenantKey("NONXM")));

        assertThrows(BusinessException.class,
            () -> provisioner.manageTenant("NEWTENANT", "ACTIVE"),
            "Only [XM] tenants allowed to manage tenant"
        );
        verify(tenantContext).getTenantKey();

    }

    @Test
    public void deleteTenant() {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(new TenantKey("XM")));
        provisioner.deleteTenant("NEWTENANT");
        verify(tenantContext).getTenantKey();
    }

    @Test
    public void deleteTenantNotAllowed() {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(new TenantKey("NONXM")));

        assertThrows(BusinessException.class,
            () -> provisioner.deleteTenant("NEWTENANT"),
            "Only [XM] tenants allowed to delete tenant"
        );
        verify(tenantContext).getTenantKey();
    }

}
