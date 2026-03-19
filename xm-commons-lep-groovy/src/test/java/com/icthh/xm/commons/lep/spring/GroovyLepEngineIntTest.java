package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import java.io.File;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
    DynamicLepTestConfig.class,
    TenantContextConfiguration.class,
    XmAuthenticationContextConfiguration.class
})
@ActiveProfiles("resolveclasstest")
public class GroovyLepEngineIntTest {

    private static final String LEP_PATH = "/config/tenants/TEST/testApp/lep/service";

    @Autowired
    private LepManagementService lepManagerService;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private DynamicTestLepService testLepService;

    @Autowired
    private XmLepScriptConfigServerResourceLoader resourceLoader;

    @Before
    public void init() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
        lepManagerService.beginThreadContext();
    }

    @After
    public void tearDown() {
        lepManagerService.endThreadContext();
    }

    @Test
    @SneakyThrows
    public void shouldCacheCompiledClassesAndReuseOnSecondCall() {
        String code = "return 'cached'";
        resourceLoader.onRefresh(LEP_PATH + "/TestLepMethodWithInput$$around.groovy", code);

        File targetDir = new File("/home/bvolokhenko/Desktop/lep-local-test");
        assertNotNull(targetDir);

        if (targetDir.exists()) {
            FileUtils.cleanDirectory(targetDir);
        }
        assertEquals(0, countClassFiles(targetDir));

        String result1 = testLepService.testLepMethod(Map.of("parameter", "value"));
        assertEquals("cached", result1);

        int classCountAfterFirstCall = countClassFiles(targetDir);
        assertTrue(classCountAfterFirstCall > 0);
        log.info("Class files after first call: {}", classCountAfterFirstCall);

        String result2 = testLepService.testLepMethod(Map.of("parameter", "value"));
        assertEquals("cached", result2);

        int classCountAfterSecondCall = countClassFiles(targetDir);
        assertEquals(classCountAfterFirstCall, classCountAfterSecondCall);
    }

    private int countClassFiles(File dir) {
        if (!dir.exists()) return 0;
        return FileUtils.listFiles(dir, new String[]{"class"}, true).size();
    }
}
