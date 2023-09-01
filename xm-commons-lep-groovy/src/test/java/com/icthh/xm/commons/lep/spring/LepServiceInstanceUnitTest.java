package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.groovy.GroovyLepEngineConfiguration;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import com.icthh.xm.lep.api.LepManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.junit.Assert.assertEquals;

/**
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
    LepTestConfig.class,
    TenantContextConfiguration.class,
    XmAuthenticationContextConfiguration.class
})
public class LepServiceInstanceUnitTest {

    @Autowired
    private LepManager lepManager;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private TestLepService lepService;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    private void setTenant(String tenantKey) {
        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        lepManager.endThreadContext();
        lepManager.beginThreadContext(ctx -> {
        });
    }

    @Test
    public void successProcessingAroundLepForTestTenant() throws Throwable {
        setTenant("test");

        String result = lepService.sayHello();
        assertEquals("ScriptWithAround.groovy around, tenant: test", result);
    }

    @Test
    public void successProcessingAroundLepForSuperTenant() throws Throwable {
        setTenant("super");

        String result = lepService.sayHello();
        assertEquals("ScriptWithAround.groovy around, tenant: super", result);
    }

    @Test
    public void successProcessingDefaultLepForUnknownTenant() throws Throwable {
        setTenant("unknown");

        String result = lepService.sayHello();
        assertEquals("ScriptWithAround.groovy default", result);
    }

    @Test
    public void successProcessingAroundLepWhileSwitchTenant() throws Throwable {
        // "super" tenant
        setTenant("super");
        String result = lepService.sayHello();
        assertEquals("ScriptWithAround.groovy around, tenant: super", result);

        // "test" tenant
        setTenant("test");
        result = lepService.sayHello();
        assertEquals("ScriptWithAround.groovy around, tenant: test", result);
    }

}
