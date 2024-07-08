package com.icthh.xm.commons.flow.rest;

import com.icthh.xm.commons.flow.context.TenantResourceLepAdditionalContext;
import com.icthh.xm.commons.flow.service.TenantResourceConfigService;
import com.icthh.xm.commons.flow.service.TenantResourceService;
import com.icthh.xm.commons.flow.service.YamlConverter;
import com.icthh.xm.commons.flow.service.resolver.TenantResourceTypeLepKeyResolver;
import com.icthh.xm.commons.flow.spec.resource.TenantResourceTypeService;
import com.icthh.xm.commons.flow.spec.step.StepSpecService;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.i18n.spring.config.LocalizationMessageProperties;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;


@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
        LepTestConfig.class,
        TenantContextConfiguration.class,
        XmAuthenticationContextConfiguration.class,
        FlowSpecResource.class,
        StepSpecService.class,
        ExceptionTranslator.class,
        LocalizationMessageService.class,
        LocalizationMessageProperties.class,
        TenantResourceTypeService.class,
        TenantResourceLepAdditionalContext.class,
        TenantResourceConfigService.class,
        TenantResourceResource.class,
        TenantResourceService.class,
        TenantResourceTypeLepKeyResolver.class,
        YamlConverter.class
    },
    properties = {"spring.application.name=testApp"}
)
@EnableWebMvc
public abstract class AbstractFlowIntTest {

    @Autowired
    WebApplicationContext wac;
    MockMvc mockMvc;
    @Autowired
    LepManagementService lepManagementService;
    @Autowired
    TenantContextHolder tenantContextHolder;

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
        lepManagementService.beginThreadContext();
    }

    @After
    public void after() {
        lepManagementService.endThreadContext();
    }

}
