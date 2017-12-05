package com.icthh.xm.commons.lep.spring;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.lep.XmGroovyExecutionStrategy;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

/**
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
    LepTestConfig.class
})
public class LepServiceInstanceUnitTest {

    @Autowired
    private SpringLepManager lepManager;

    @Mock
    private TenantContext tenantContext;

    @Autowired
    private TestLepService lepService;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContext);
        });
    }

    @Test
    public void successProcessingAroundLepForTestTenant() throws Throwable {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("test")));

        String result = lepService.sayHello();
        assertEquals("ScriptWithAround.groovy around, tenant: test", result);
    }

    @Test
    public void successProcessingAroundLepForSuperTenant() throws Throwable {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("super")));

        String result = lepService.sayHello();
        assertEquals("ScriptWithAround.groovy around, tenant: super", result);
    }

    @Test
    public void successProcessingDefaultLepForUnknownTenant() throws Throwable {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("unknown")));

        String result = lepService.sayHello();
        assertEquals("ScriptWithAround.groovy default", result);
    }

    @Test
    public void successProcessingAroundLepWhileSwitchTenant() throws Throwable {
        // "super" tenant
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("super")));
        String result = lepService.sayHello();
        assertEquals("ScriptWithAround.groovy around, tenant: super", result);

        // "test" tenant
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("test")));
        result = lepService.sayHello();
        assertEquals("ScriptWithAround.groovy around, tenant: test", result);
    }

}
