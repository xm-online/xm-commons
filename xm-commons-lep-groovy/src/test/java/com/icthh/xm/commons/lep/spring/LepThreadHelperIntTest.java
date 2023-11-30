package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.security.internal.XmAuthentication;
import com.icthh.xm.commons.security.internal.XmAuthenticationDetails;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.icthh.xm.commons.lep.spring.DynamicLepClassResolveIntTest.loadFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
    DynamicLepTestConfig.class,
    TenantContextConfiguration.class,
    XmAuthenticationContextConfiguration.class
})
@ActiveProfiles("resolveclasstest")
public class LepThreadHelperIntTest {

    @Autowired
    private LepManagementService lepManager;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private DynamicTestLepService testLepService;

    @Autowired
    private XmLepScriptConfigServerResourceLoader resourceLoader;

    @Before
    public void init() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");

        var authorities = List.of(new SimpleGrantedAuthority("SUPER-ADMIN"));
        XmAuthentication auth = new XmAuthentication(mock(XmAuthenticationDetails.class), "", authorities);
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));

        lepManager.beginThreadContext();
    }

    @Test
    @SneakyThrows
    public void testRunLepInBackground() {
        String body = loadFile("lep/LepWithThread.groovy");
        String threadBody = loadFile("lep/LepThreadBody.groovy");

        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/service/TestLepMethod$$around.groovy", threadBody);
        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/service/TestLepMethodWithInput$$around.groovy", body);
        String result = testLepService.testLepMethod(Map.of("testLepService", testLepService));
        assertEquals("test", result);
    }

    @Test
    @SneakyThrows
    public void testBackwardCompatibilityOfRunInThread() {
        String threadUtils = loadFile("lep/Commons$$threadUtils$$around.groovy");
        String threadBody = loadFile("lep/TestLepInBackground.groovy");
        String code = loadFile("lep/Commons$$service$$around.groovy");

        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/commons/Commons$$service$$around.groovy", code);
        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/commons/Commons$$threadUtils$$around.groovy", threadUtils);
        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/service/TestLepMethodWithInput$$around.groovy", threadBody);
        AtomicBoolean lepResult = new AtomicBoolean(false);
        testLepService.testLepMethod(Map.of("result", lepResult));
        assertTrue(lepResult.get());
    }

}
