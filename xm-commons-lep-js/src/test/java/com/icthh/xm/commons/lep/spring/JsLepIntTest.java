package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.InputStream;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    JsLepTestConfig.class,
    TenantContextConfiguration.class,
    XmAuthenticationContextConfiguration.class
})
@TestPropertySource(properties = {"spring.application.name=testApp", "debug=true"})
public class JsLepIntTest {

    @Autowired
    private LepManagementService lepManagementService;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authenticationContextHolder;

    @Autowired
    private JsTestLepService testLepService;

    @Autowired
    private XmLepScriptConfigServerResourceLoader resourceLoader;

    @BeforeEach
    public void init() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
        lepManagementService.beginThreadContext();
    }

    @AfterEach
    public void after() {
        lepManagementService.endThreadContext();
    }

    @Test
    @SneakyThrows
    public void testJsLeps() {
        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/service/TestLepMethod.js",
            loadFile("lep/testLepMethod.js"));
        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/service/TestLepMethodWithInput.js",
            loadFile("lep/testLepMethodWithInput.js"));
        String result = testLepService.testLepMethod(Map.of("testLepService", testLepService));
        assertEquals("Hello JSTEST", result);
    }

    @Test
    public void testJsWithResolver() {
        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/service/TestLepMethodWithResolver__MY.KEY.js",
            loadFile("lep/testLepMethod.js"));
        String result = testLepService.testLepMethodWithResolver("MY.KEY");
        assertEquals("JSTEST", result);
    }

    @SneakyThrows
    public static String loadFile(String path) {
        try (InputStream cfgInputStream = new ClassPathResource(path).getInputStream()) {
            return IOUtils.toString(cfgInputStream, UTF_8);
        }
    }
}

