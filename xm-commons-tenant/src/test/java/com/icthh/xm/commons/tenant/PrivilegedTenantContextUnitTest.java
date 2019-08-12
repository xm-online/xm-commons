package com.icthh.xm.commons.tenant;

import static org.junit.Assert.assertEquals;

import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Supplier;

/**
 * The {@link PrivilegedTenantContextUnitTest} class.
 */
public class PrivilegedTenantContextUnitTest {

    private TenantContextHolder tenantContextHolder;

    @Before
    public void before() {
        tenantContextHolder = new DefaultTenantContextHolder();
    }

    @After
    public void destroy() {
        if (tenantContextHolder != null) {
            tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
            tenantContextHolder = null;
        }
    }

    private PrivilegedTenantContext getPrivilegedTenantContext() {
        return tenantContextHolder.getPrivilegedContext();
    }

    private TenantContext getTenantContext() {
        return tenantContextHolder.getPrivilegedContext();
    }

    private static Tenant buildTenant(String name) {
        return new PlainTenant(TenantKey.valueOf(name));
    }

    private String getCurrentTenant() {
        return TenantContextUtils.getRequiredTenantKeyValue(getTenantContext());
    }

    @Test
    public void successTenantContextSwitchOneLevel() {
        PrivilegedTenantContext privilegedTenantContext = getPrivilegedTenantContext();
        privilegedTenantContext.setTenant(buildTenant("tenant-A"));
        privilegedTenantContext.execute(buildTenant("tenant-B"), (Supplier<Void>) () -> {
            assertEquals("tenant-B", getCurrentTenant());
            return null;
        });
        assertEquals("tenant-A", getCurrentTenant());
    }

}
