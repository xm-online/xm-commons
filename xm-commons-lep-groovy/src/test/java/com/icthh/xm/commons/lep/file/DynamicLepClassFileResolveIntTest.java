package com.icthh.xm.commons.lep.file;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.spring.DynamicLepTestFileConfig;
import com.icthh.xm.commons.lep.spring.DynamicTestLepService;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

/**
 *
 */
@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
        DynamicLepTestFileConfig.class,
        TenantContextConfiguration.class,
        XmAuthenticationContextConfiguration.class
})
@ActiveProfiles("resolvefiletest")
public class DynamicLepClassFileResolveIntTest {

    @Autowired
    private LepManagementService lepManager;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private DynamicTestLepService testLepService;

    @Autowired
    private TenantAliasService tenantAliasService;

    @Autowired
    private TemporaryFolder folder;

    @Before
    @SneakyThrows
    public void init() {
        MockitoAnnotations.initMocks(this);

        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
        lepManager.beginThreadContext();

        FileUtils.cleanDirectory(folder.getRoot());
    }

    @After
    public void destroy() {
        lepManager.endThreadContext();
    }

    @Test
    @SneakyThrows
    public void testResolvingClassFromCommons() {
        runTest("msCommons", "TEST.testApp.lep.commons.folder", "TEST/testApp/lep/commons/folder");
    }

    @Test
    @SneakyThrows
    public void testResolvingClassFromParentTenant() {
        tenantAliasService.onRefresh(loadFile("lep/TenantAlias.yml"));
        runTest("msCommons", "PARENT.testApp.lep.commons.folder", "TEST/testApp/lep/commons/folder");
    }

    @Test
    @SneakyThrows
    public void testUpdateFileLep() {

        runTest("msCommons", "TEST.testApp.lep.commons.folder", "TEST/testApp/lep/commons/folder");
        FileUtils.cleanDirectory(folder.getRoot());
        createFile("/config/tenants/TEST/testApp/lep/service/TestLepMethod$$around.groovy", "return '1'");
        // this sleep is needed because groovy has debounce time to lep update
        Thread.sleep(110);
        String result = testLepService.testLepMethod();
        assertEquals("1", result);
    }

    @Test
    @SneakyThrows
    public void testLepFromParentTenant() {
        tenantAliasService.onRefresh(loadFile("lep/TenantAlias.yml"));
        String testString = "Hello from parent lep";
        createFile("/config/tenants/PARENT/testApp/lep/service/TestLepMethod$$around.groovy", "return '" + testString + "'");
        // this sleep is needed because groovy has debounce time to lep update
        Thread.sleep(110);
        String result = testLepService.testLepMethod();
        assertEquals(testString, result);
    }

    @Test
    @SneakyThrows
    public void testResolvingClassFromTenantLevelCommons() {
        runTest("tenantCommons", "TEST.commons.lep.folder", "TEST/commons/lep/folder");
    }

    @Test
    @SneakyThrows
    public void testResolvingClassFromEnvLevelCommons() {
        runTest("envCommons", "commons.lep.folder", "commons/lep/folder");
    }

    @Test
    @SneakyThrows
    public void runEnvCommons() {
        createFile("/config/tenants/commons/lep/Commons$$testEnvCommons$$around.groovy",
            "return 'envCommonsWorks'"
        );
        createFile("/config/tenants/TEST/testApp/lep/service/TestLepMethod$$around.groovy",
            "return lepContext.commons.environment.testEnvCommons()"
        );
        Thread.sleep(110);
        String result = testLepService.testLepMethod();
        assertEquals("envCommonsWorks", result);
    }

    @Test
    @SneakyThrows
    public void runTenantCommons() {
        createFile("/config/tenants/TEST/commons/lep/Commons$$testTenantCommons$$around.groovy",
            "return 'tenantCommonsWorks'"
        );
        createFile("/config/tenants/TEST/testApp/lep/service/TestLepMethod$$around.groovy",
            "return lepContext.commons.tenant.testTenantCommons()"
        );
        Thread.sleep(110);
        String result = testLepService.testLepMethod();
        assertEquals("tenantCommonsWorks", result);
    }

    @Test
    @SneakyThrows
    public void runCommons() {
        createFile("/config/tenants/TEST/testApp/lep/commons/Commons$$testTenantCommons$$around.groovy",
            "return 'tenantCommonsWorks'"
        );
        createFile("/config/tenants/TEST/testApp/lep/service/TestLepMethod$$around.groovy",
            "return lepContext.commons.testTenantCommons()"
        );
        Thread.sleep(110);
        String result = testLepService.testLepMethod();
        assertEquals("tenantCommonsWorks", result);
    }

    @Test
    @SneakyThrows
    public void runCreateService() {
        createFile("/config/tenants/TEST/testApp/lep/commons/TestFileService.groovy",
            "" +
                "package TEST.testApp.lep.commons\n" +
                "class TestFileService { def hello() {'fileServiceWorks'} }\n"
        );
        createFile("/config/tenants/TEST/testApp/lep/service/TestLepMethod$$around.groovy",
            "" +
                "import TEST.testApp.lep.commons.TestFileService\n" +
                "return lepContext.lepServices.getInstance(TestFileService.class).hello()"
        );
        Thread.sleep(110);
        String result = testLepService.testLepMethod();
        assertEquals("fileServiceWorks", result);
    }

    private void runTest(String suffix, String packageName, String path) throws InterruptedException {
        createFile("/config/tenants/TEST/testApp/lep/service/TestLepMethod$$around.groovy",
                loadFile("lep/TestClassUsage")
                        .replace("${package}", packageName)
                        .replace("${suffix}", suffix)
        );
        String testClassDeclarationPath = "/config/tenants/" + path + "/TestClassDeclaration" + suffix + "$$tenant.groovy";
        String testClassBody = loadFile("lep/TestClassDeclaration")
                .replace("${package}", packageName)
                .replace("${suffix}", suffix)
                .replace("${value}", "I am class in lep!");
        createFile(testClassDeclarationPath, testClassBody);

        // this sleep is needed because groovy has debounce time to lep update
        Thread.sleep(110);
        String result = testLepService.testLepMethod();
        assertEquals("I am class in lep!", result);

        createFile(testClassDeclarationPath,
                loadFile("lep/TestClassDeclaration")
                        .replace("${package}", packageName)
                        .replace("${suffix}", suffix)
                        .replace("${value}", "I am updated class in lep!"));

        // this sleep is needed because groovy has debounce time to lep update
        Thread.sleep(110);
        result = testLepService.testLepMethod();
        assertEquals("I am updated class in lep!", result);
    }

    @SneakyThrows
    private void createFile(String path, String content) {
        File file = new File(folder.getRoot().toPath().toFile().getAbsolutePath() + path);
        file.getParentFile().mkdirs();
        file.createNewFile();
        FileUtils.writeStringToFile(file, content, UTF_8);
        log.info("Path to file {}", file.getAbsolutePath());
    }

    @SneakyThrows
    public static String loadFile(String path) {
        try (InputStream cfgInputStream = new ClassPathResource(path).getInputStream()) {
            return IOUtils.toString(cfgInputStream, UTF_8);
        }
    }

}
