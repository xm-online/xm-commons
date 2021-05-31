package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.GroovyFileParser;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantKey;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.tools.groovydoc.GroovyDocTool;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
        DynamicLepTestConfig.class
})
@ActiveProfiles("resolveclasstest")
public class DynamicLepClassResolveIntTest {

    @Autowired
    private SpringLepManager lepManager;

    @Mock
    private TenantContext tenantContext;

    @Autowired
    private DynamicTestLepService testLepService;

    @Autowired
    private XmLepScriptConfigServerResourceLoader resourceLoader;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContext);
        });
    }

    @Test
    @SneakyThrows
    public void testResolvingClassFromCommons() {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));
        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/service/TestLepMethod$$around.groovy",
                loadFile("lep/TestClassUsage"));
        String testClassDeclarationPath = "/config/tenants/TEST/testApp/lep/commons/folder/TestClassDeclaration$$tenant.groovy";
        String testClassBody = loadFile("lep/TestClassDeclaration").replace("${value}", "I am class in lep!");
        resourceLoader.onRefresh(testClassDeclarationPath, testClassBody);

        String result = testLepService.testLepMethod();
        assertEquals("I am class in lep!", result);

        // this sleep is needed because groovy has debounce time to lep update
        Thread.sleep(100);
        resourceLoader.onRefresh(testClassDeclarationPath,
                loadFile("lep/TestClassDeclaration").replace("${value}", "I am updated class in lep!"));
        result = testLepService.testLepMethod();
        assertEquals("I am updated class in lep!", result);

    }

    @SneakyThrows
    private static String loadFile(String path) {
        try (InputStream cfgInputStream = new ClassPathResource(path).getInputStream()) {
            return IOUtils.toString(cfgInputStream, UTF_8);
        }
    }

}
