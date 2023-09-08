package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.icthh.xm.commons.config.client.service.TenantAliasService.TENANT_ALIAS_CONFIG;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
        DynamicLepTestConfig.class,
        TenantContextConfiguration.class,
        XmAuthenticationContextConfiguration.class
})
@ActiveProfiles("resolveclasstest")
public class DynamicLepClassResolveIntTest {

    @Autowired
    private LepManagementService lepManagementService;

    @Mock
    private TenantContext tenantContext;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private DynamicTestLepService testLepService;

    @Autowired
    private TenantAliasService tenantAliasService;

    @Autowired
    private XmLepScriptConfigServerResourceLoader resourceLoader;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
        lepManagementService.beginThreadContext();
    }

    @After
    public void after() {
        lepManagementService.endThreadContext();
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
        refreshLep("/config/tenants/commons/lep/Commons$$testEnvCommons$$around.groovy",
            "return 'envCommonsWorks'"
        );
        refreshLep("/config/tenants/TEST/testApp/lep/service/TestLepMethod$$around.groovy",
            "return lepContext.commons.environment.testEnvCommons()"
        );
        String result = testLepService.testLepMethod();
        assertEquals("envCommonsWorks", result);
    }

    @Test
    @SneakyThrows
    public void runTenantCommons() {
        refreshLep("/config/tenants/TEST/commons/lep/Commons$$testTenantCommons$$around.groovy",
            "return 'tenantCommonsWorks'"
        );
        refreshLep("/config/tenants/TEST/testApp/lep/service/TestLepMethod$$around.groovy",
            "return lepContext.commons.tenant.testTenantCommons()"
        );
        String result = testLepService.testLepMethod();
        assertEquals("tenantCommonsWorks", result);
    }

    @Test
    @SneakyThrows
    public void runCommons() {
        refreshLep("/config/tenants/TEST/testApp/lep/commons/Commons$$testTenantCommons$$around.groovy",
            "return 'tenantCommonsWorks'"
        );
        refreshLep("/config/tenants/TEST/testApp/lep/service/TestLepMethod$$around.groovy",
            "return lepContext.commons.testTenantCommons()"
        );
        String result = testLepService.testLepMethod();
        assertEquals("tenantCommonsWorks", result);
    }

    public void refreshLep(String path, String content) {
        resourceLoader.onRefresh(path, content);
        resourceLoader.refreshFinished(List.of(path));
    }

    @Test
    @SneakyThrows
    public void testEnumInterfaceAnnotationResolving() {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));

        loadDeclarationLep("TestEnumDeclaration");
        loadDeclarationLep("TestAnnotationDeclaration");
        loadDeclarationLep("TestInterfaceDeclaration");
        loadDeclarationLep("TestEnumInterfaceAnnotationDeclaration");
        refreshLep("/config/tenants/TEST/testApp/lep/service/TestLepMethod$$around.groovy",
            loadFile("lep/TestEnumInterfaceAnnotationUsage")
        );
        log.info("Run lep method");
        String result = testLepService.testLepMethod();
        assertEquals("VAL1", result);
    }

    private void loadDeclarationLep(String testEnumDeclaration) {
        String testClassDeclarationPath = "/config/tenants/TEST/testApp/lep/commons/folder/" + testEnumDeclaration + "$$tenant.groovy";
        String testClassBody = loadFile("lep/" + testEnumDeclaration);
        refreshLep(testClassDeclarationPath, testClassBody);
    }


    private void runTest(String suffix, String packageName, String path) {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));

        String testClassDeclarationPath = "/config/tenants/" + path + "/TestClassDeclaration" + suffix + "$$tenant.groovy";
        String testClassBody = loadFile("lep/TestClassDeclaration")
                .replace("${package}", packageName)
                .replace("${suffix}", suffix)
                .replace("${value}", "I am class in lep!");
        refreshLep(testClassDeclarationPath, testClassBody);
        refreshLep("/config/tenants/TEST/testApp/lep/service/TestLepMethod$$around.groovy",
            loadFile("lep/TestClassUsage")
                .replace("${package}", packageName)
                .replace("${suffix}", suffix)
        );

        String result = testLepService.testLepMethod();
        assertEquals("I am class in lep!", result);

        refreshLep(testClassDeclarationPath,
                loadFile("lep/TestClassDeclaration")
                        .replace("${package}", packageName)
                        .replace("${suffix}", suffix)
                        .replace("${value}", "I am updated class in lep!"));

        result = testLepService.testLepMethod();
        assertEquals("I am updated class in lep!", result);
    }

    @Test
    @SneakyThrows
    public void testReloadLepClass() {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));

        String testClassDeclarationPath = "/config/tenants/commons/lep/folder/TestClassDeclarationReloadClass$$tenant.groovy";
        String testClassBody = loadFile("lep/TestClassDeclaration")
            .replace("${package}", "commons.lep.folder")
            .replace("${suffix}", "ReloadClass")
            .replace("${value}", "I am class in lep!");
        refreshLep(testClassDeclarationPath, testClassBody);

        refreshLep("/config/tenants/TEST/testApp/lep/service/TestLepMethod$$around.groovy",
                loadFile("lep/TestLoadClassByName")
                        .replace("${package}", "commons.lep.folder")
                        .replace("${suffix}", "ReloadClass")
        );

        String result = testLepService.testLepMethod();
        assertEquals("I am class in lep!", result);

        refreshLep(testClassDeclarationPath,
                loadFile("lep/TestClassDeclaration")
                        .replace("${package}", "commons.lep.folder")
                        .replace("${suffix}", "ReloadClass")
                        .replace("${value}", "I am updated class in lep!"));
        // this sleep is needed because groovy has debounce time to lep update
        Thread.sleep(110);
        result = testLepService.testLepMethod();
        assertEquals("I am updated class in lep!", result);
    }

    @Test
    @SneakyThrows
    public void testLepServiceFactory() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));

        refreshLep("/config/tenants/TEST/testApp/lep/service/TestLepMethodWithInput$$around.groovy",
                loadFile("lep/TestLepServiceMethod")
        );

        String testClassDeclarationPath = "/config/tenants/TEST/testApp/lep/commons/TestLepServiceDeclaration$$tenant.groovy";
        String testClassBody = loadFile("lep/TestLepServiceDeclaration");
        refreshLep(testClassDeclarationPath, testClassBody);

        AtomicInteger countConstructorCall = new AtomicInteger();

        String result = testLepService.testLepMethod(Map.of(
                "countConstructorCall", countConstructorCall,
                "testString", "It_works"
        ));
        assertEquals("It_works", result);
        assertEquals(1, countConstructorCall.get());

        result = testLepService.testLepMethod(Map.of(
                "countConstructorCall", countConstructorCall,
                "testString", "New_argument"
        ));
        // checks that argument not updated, constructor was not called
        assertEquals("It_works", result);
        assertEquals(1, countConstructorCall.get());

        refreshLep(testClassDeclarationPath, testClassBody);
        result = testLepService.testLepMethod(Map.of(
                "countConstructorCall", countConstructorCall,
                "testString", "New_argument"
        ));
        // checks service recreated after refresh
        assertEquals(2, countConstructorCall.get());
        assertEquals("New_argument", result);
    }

    @Test
    @SneakyThrows
    public void testLepWithAnotherLepDependencies() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));

        // 18 - because default count of buckets in hashmap is 16, and on 18 two object will be in one bucket
        // and in this case if we use recursive computeIfAbsent, that we get recursive update exception
        for (int i = 0; i < 18; i++) {
            loadService(i);
        }

        String testClassDeclarationPath1 = "/config/tenants/TEST/testApp/lep/commons/AnotherLepService18$$tenant.groovy";
        String testClassBody1 = loadFile("lep/AnotherLepService")
            .replace("#className#", "AnotherLepService18")
            .replace("#count#", "18")
            .replace("#dependsOn#", "AnotherLepService1");
        refreshLep(testClassDeclarationPath1, testClassBody1);

        String testClassDeclarationPath = "/config/tenants/TEST/testApp/lep/commons/TestLepServiceDependsOfAnotherLepService$$tenant.groovy";
        String testClassBody = loadFile("lep/TestLepServiceDependsOfAnotherLepService");
        refreshLep(testClassDeclarationPath, testClassBody);

        refreshLep("/config/tenants/TEST/testApp/lep/service/TestLepMethod$$around.groovy",
            loadFile("lep/TestLepWithAnotherLepMethod")
        );

        String result = testLepService.testLepMethod();
        assertEquals("TEST.testApp.lep.commons.AnotherLepService1", result);
    }

    private void loadService(int i) {
        String testClassDeclarationPath1 = "/config/tenants/TEST/testApp/lep/commons/AnotherLepService" + i + "$$tenant.groovy";
        String testClassBody1 = loadFile("lep/AnotherLepService")
            .replace("#className#", "AnotherLepService" + i)
            .replace("#count#", String.valueOf(i))
            .replace("#dependsOn#", "AnotherLepService" + (i + 1));
        refreshLep(testClassDeclarationPath1, testClassBody1);
    }

    @SneakyThrows
    public static String loadFile(String path) {
        try (InputStream cfgInputStream = new ClassPathResource(path).getInputStream()) {
            return IOUtils.toString(cfgInputStream, UTF_8);
        }
    }

}
