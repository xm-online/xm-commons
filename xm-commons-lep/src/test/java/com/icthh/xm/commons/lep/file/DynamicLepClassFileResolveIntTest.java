package com.icthh.xm.commons.lep.file;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import static com.icthh.xm.commons.config.client.service.TenantAliasService.TENANT_ALIAS_CONFIG;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import com.icthh.xm.commons.lep.spring.DynamicLepTestFileConfig;
import com.icthh.xm.commons.lep.spring.DynamicTestLepService;
import com.icthh.xm.commons.lep.spring.SpringLepManager;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import static java.nio.charset.StandardCharsets.UTF_8;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;

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
    private SpringLepManager lepManager;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private XmAuthenticationContext authContext;

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

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContext);
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContext);
        });

        FileUtils.cleanDirectory(folder.getRoot());
    }

    @Test
    @SneakyThrows
    public void testResolvingClassFromCommons() {
        runTest("msCommons", "TEST.testApp.lep.commons.folder", "TEST/testApp/lep/commons/folder");
    }

    @Test
    @SneakyThrows
    public void testResolvingClassFromParentTenant() {
        tenantAliasService.onRefresh(TENANT_ALIAS_CONFIG, loadFile("lep/TenantAlias.yml"));
        runTest("msCommons", "PARENT.testApp.lep.commons.folder", "TEST/testApp/lep/commons/folder");
    }

    @Test
    @SneakyThrows
    public void testUpdateFileLep() {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));

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
        Assume.assumeTrue(!SystemUtils.IS_OS_WINDOWS);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));
        tenantAliasService.onRefresh(TENANT_ALIAS_CONFIG, loadFile("lep/TenantAlias.yml"));
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

    private void runTest(String suffix, String packageName, String path) throws InterruptedException {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));
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
        boolean newFile = file.createNewFile();
        if (!newFile) {
            log.warn("File exist {} was not created", file.getAbsolutePath());
        }
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
