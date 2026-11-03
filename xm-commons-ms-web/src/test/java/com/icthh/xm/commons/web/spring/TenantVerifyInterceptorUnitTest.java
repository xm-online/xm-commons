package com.icthh.xm.commons.web.spring;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.tenant.PlainTenant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantVerifyInterceptorUnitTest {

    private static final String TENANTS_LIST = """
        {"entity": [{"name": "suspended", "state": "SUSPENDED"}, {"name": "active", "state": "ACTIVE"}]}
        """;

    private final TenantContextHolder tenantContextHolder = new DefaultTenantContextHolder();
    private final TenantVerifyInterceptor interceptor = new TenantVerifyInterceptor(
        new TenantListRepository(null, new Configuration("/config/tenants/tenants-list.json", TENANTS_LIST),
                                 "entity", new XmConfigProperties()),
        tenantContextHolder);

    @AfterEach
    void destroyTenantContext() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    void rejectsSuspendedTenantWhateverTheCaseOfTheKey() throws Exception {
        setTenant("SUSPENDED");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(new MockHttpServletRequest(), response, null));
        assertEquals(400, response.getStatus());
    }

    @Test
    void allowsActiveTenant() throws Exception {
        setTenant("ACTIVE");

        assertTrue(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), null));
    }

    private void setTenant(String tenantKey) {
        tenantContextHolder.getPrivilegedContext().setTenant(new PlainTenant(TenantKey.valueOf(tenantKey)));
    }
}
