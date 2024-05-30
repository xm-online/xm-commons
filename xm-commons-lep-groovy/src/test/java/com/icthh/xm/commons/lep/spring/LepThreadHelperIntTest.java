package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.icthh.xm.commons.lep.spring.DynamicLepClassResolveIntTest.loadFile;
import static java.util.Collections.emptySet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        var token = new UsernamePasswordAuthenticationToken("xm", "N/A", authorities);
        var request = new OAuth2Request(Map.of(), "webapp", authorities, true, emptySet(),
                emptySet(), null, emptySet(), Map.of());
        OAuth2Authentication auth = new OAuth2Authentication(request, token);
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));

        lepManager.beginThreadContext();
    }

    @After
    public void after() {
        lepManager.endThreadContext();
    }

    @Test
    @SneakyThrows
    public void testRunLepInBackground() {
        String body = loadFile("lep/LepWithThread.groovy");
        String threadBody = loadFile("lep/LepThreadBody.groovy");

        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/service/TestLepMethod$$around.groovy", threadBody);
        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/service/TestLepMethodWithInput$$around.groovy", body);
        String result = testLepService.testLepMethod(Map.of("testLepService", testLepService));
        assertEquals("TEST", result);
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
