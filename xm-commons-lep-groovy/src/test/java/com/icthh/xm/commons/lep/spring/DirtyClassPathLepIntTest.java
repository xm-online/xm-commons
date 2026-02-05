package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.InputStream;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

/**
 *
 */
@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    DynamicDirtyClassPathLepTestConfig.class,
    TenantContextConfiguration.class,
    XmAuthenticationContextConfiguration.class
})
@ActiveProfiles("resolvedirtytest")
@TestPropertySource(properties = {
    "application.lep.recreate-groovy-engine-on-refresh=false"
})
public class DirtyClassPathLepIntTest {

    @Autowired
    private LepManagementService lepManager;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private DynamicTestLepService testLepService;

    @Autowired
    private XmLepScriptConfigServerResourceLoader resourceLoader;

    @BeforeEach
    @SneakyThrows
    public void init() {
        MockitoAnnotations.initMocks(this);

        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
        lepManager.beginThreadContext();
    }

    @AfterEach
    public void destroy() {
        lepManager.endThreadContext();
    }

    @Test
    @SneakyThrows
    public void runCreateService() {
        refreshLep("/config/tenants/TEST/testApp/lep/commons/StaticHolder.groovy",
            "" +
                "package TEST.testApp.lep.commons\n" +
                "class StaticHolder { public static String savedName = 'noName' }\n"
        );
        refreshLep("/config/tenants/TEST/testApp/lep/commons/TestService.groovy",
            "" +
                "package TEST.testApp.lep.commons\n" +
                "class TestService { \n" +
                "   def hello(TestDto dto) {" +
                "       String result = \"${StaticHolder.savedName}_${dto.name}_testServiceWorks\"; " +
                "       StaticHolder.savedName = dto.name; " +
                "       return result;" +
                "   } " +
                "}\n"
        );
        refreshLep("/config/tenants/TEST/testApp/lep/commons/TestDto.groovy",
            "" +
                "package TEST.testApp.lep.commons\n" +
                "class TestDto { String name = 'origin' }\n"
        );
        refreshLep("/config/tenants/TEST/testApp/lep/service/TestLepMethod$$around.groovy",
            "" +
                "import TEST.testApp.lep.commons.TestService\n" +
                "import TEST.testApp.lep.commons.TestDto\n" +
                "return new TestService().hello(new TestDto())"
        );
        String result = testLepService.testLepMethod();
        assertEquals("noName_origin_testServiceWorks", result);

        result = testLepService.testLepMethod();
        assertEquals("origin_origin_testServiceWorks", result);

        refreshLep("/config/tenants/TEST/testApp/lep/commons/TestDto.groovy",
            "" +
                "package TEST.testApp.lep.commons\n" +
                "class TestDto { String name = 'refreshed' }\n"
        );
        Thread.sleep(110); // groovy reread config ones per 100ms lep
        result = testLepService.testLepMethod();
        assertEquals("StaticHolder was refreshed!", "origin_refreshed_testServiceWorks", result);
    }

    public void refreshLep(String path, String content) {
        resourceLoader.onRefresh(path, content);
        resourceLoader.refreshFinished(List.of(path));
    }

    @SneakyThrows
    public static String loadFile(String path) {
        try (InputStream cfgInputStream = new ClassPathResource(path).getInputStream()) {
            return IOUtils.toString(cfgInputStream, UTF_8);
        }
    }

}
