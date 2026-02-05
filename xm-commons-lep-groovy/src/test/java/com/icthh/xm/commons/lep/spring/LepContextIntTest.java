package com.icthh.xm.commons.lep.spring;

import static com.icthh.xm.commons.lep.spring.DynamicLepClassResolveIntTest.loadFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.spring.TestLepContextCustomizer.CustomTestLepContext;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    DynamicLepTestConfig.class,
    LepContextIntTest.TestCustomizationConfiguration.class,
    TenantContextConfiguration.class,
    XmAuthenticationContextConfiguration.class
})
@ActiveProfiles({"resolveclasstest", "lepContextCustomizer"})
public class LepContextIntTest {

    @Autowired
    private LepManagementService lepManagerService;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private DynamicTestLepService testLepService;

    @Autowired
    private XmLepScriptConfigServerResourceLoader resourceLoader;

    @BeforeEach
    public void init() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
        lepManagerService.beginThreadContext();
    }

    @Test
    public void testLepContextCustomizer() {
        String code = loadFile("lep/TestUseAsLepContextBase.groovy");
        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/service/TestLepMethodWithInputObject.groovy", code);
        String codeInside = loadFile("lep/TestUseAsLepContextInside.groovy");
        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/service/TestUseAsLepContext$$around.groovy", codeInside);

        Object result = testLepService.testLepMethodObject(Map.of("service", testLepService));
        assertTrue(result instanceof CustomTestLepContext);
        CustomTestLepContext custom = (CustomTestLepContext) result;
        log.info("class {}", custom.originalLepContext.getClass().getCanonicalName());
        assertTrue(custom.originalLepContext instanceof CustomTestLepContext);
        assertEquals(2, custom.count);
    }

    @Configuration
    @Profile("lepContextCustomizer")
    public static class TestCustomizationConfiguration {

        @Bean
        public LepContextCustomizer testLepContextCustomizer() {
            return new TestLepContextCustomizer();
        }

        @Bean
        @Primary
        public LepContextCustomizer groovyMapLepWrapperFactory() {
            return (lepContext, lepEngine, lepMethod) -> lepContext;
        }
    }


}
