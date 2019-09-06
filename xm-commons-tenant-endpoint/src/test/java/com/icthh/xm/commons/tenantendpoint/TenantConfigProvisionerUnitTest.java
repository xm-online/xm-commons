package com.icthh.xm.commons.tenantendpoint;

import static com.icthh.xm.commons.config.domain.Configuration.of;
import static com.icthh.xm.commons.tenantendpoint.TenantConfigProvisioner.builder;
import static com.icthh.xm.commons.tenantendpoint.TenantConfigProvisioner.prependTenantPath;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.gen.model.Tenant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

public class TenantConfigProvisionerUnitTest {

    private TenantConfigProvisioner tenantConfigProvisioner;

    private static final String TENANT_KEY = "testKey";

    @Mock
    TenantConfigRepository tenantConfigRepository;

    @Before
    public void setup() {

        MockitoAnnotations.initMocks(this);

        tenantConfigProvisioner = builder()
            .tenantConfigRepository(tenantConfigRepository)
            .configuration(of().path(prependTenantPath("/uaa/dir/file1.txt")).content("content 1").build())
            .configuration(of().path(prependTenantPath("file2.txt")).content("content 2").build())
            .build();

    }

    @Test
    public void testPrependTenantPath() {

        assertEquals("/config/tenants/{tenantName}/uaa/dir/file1.txt", prependTenantPath("/uaa/dir/file1.txt"));
        assertEquals("/config/tenants/{tenantName}/uaa/file2.txt", prependTenantPath("uaa/file2.txt"));
        assertEquals("/config/tenants/{tenantName}/file4.txt", prependTenantPath("file4.txt"));
        assertEquals("/config/tenants/{tenantName}", prependTenantPath(""));
        assertEquals("/config/tenants/{tenantName}", prependTenantPath("/"));

    }

    @Test
    public void createTenant() {

        Tenant tenant = new Tenant().tenantKey(TENANT_KEY);

        tenantConfigProvisioner.createTenant(tenant);

        List<Configuration> expected = new ArrayList<>();
        expected.add(of().path("/config/tenants/{tenantName}/uaa/dir/file1.txt").content("content 1").build());
        expected.add(of().path("/config/tenants/{tenantName}/file2.txt").content("content 2").build());

        verify(tenantConfigRepository).createConfigsFullPath(eq(TENANT_KEY), eq(expected));

    }

    @Test
    public void manageTenant() {
        tenantConfigProvisioner.manageTenant(TENANT_KEY, "SUSPEND");
        verifyZeroInteractions(tenantConfigRepository);
    }

    @Test
    public void deleteTenant() {
    }
}
