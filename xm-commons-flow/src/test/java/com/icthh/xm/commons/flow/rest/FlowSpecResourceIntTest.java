package com.icthh.xm.commons.flow.rest;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.flow.context.TenantResourceLepAdditionalContext;
import com.icthh.xm.commons.flow.service.TenantResourceConfigService;
import com.icthh.xm.commons.flow.service.TenantResourceService;
import com.icthh.xm.commons.flow.spec.resource.TenantResourceTypeService;
import com.icthh.xm.commons.flow.spec.step.StepSpecService;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.i18n.spring.config.LocalizationMessageProperties;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static com.icthh.xm.commons.flow.steps.StepsRefreshableConfigurationUnitTest.loadFile;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


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
        TenantResourceService.class
    },
    properties = {"spring.application.name=testApp"}
)
@EnableWebMvc
public class FlowSpecResourceIntTest {

    @Autowired
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    @Autowired
    private StepSpecService stepSpecService;
    @Autowired
    private TenantResourceTypeService tenantResourceTypeService;
    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private TestLepService testLepService;

    @Autowired
    private XmLepScriptConfigServerResourceLoader lep;
    @Autowired
    private LepManagementService lepManagementService;

    @MockBean
    private TenantConfigRepository tenantConfigRepository;
    @Autowired
    private TenantResourceConfigService tenantResourceConfigService;

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
    }

    @Test
    @SneakyThrows
    public void testListOfSteps() {
        stepSpecService.onRefresh("/config/tenants/TEST/testApp/step-spec/testreadspec.yml", loadFile("step-spec/testreadspec.yml"));
        stepSpecService.onRefresh("/config/tenants/TEST/testApp/step-spec/anothersteps.yml", loadFile("step-spec/anothersteps.yml"));
        mockMvc.perform(get("/api/flow/spec/steps"))
            .andDo(print())
            .andExpect(jsonPath("$.[0].key").value("othercondition"))
            .andExpect(jsonPath("$.[0].type").value("CONDITION"))
            .andExpect(jsonPath("$.[1].key").value("actionkey"))
            .andExpect(jsonPath("$.[1].type").value("ACTION"))
            .andExpect(jsonPath("$.[2].key").value("otheraction"))
            .andExpect(jsonPath("$.[2].type").value("ACTION"))

            .andExpect(jsonPath("$.[2].resources.[0].key").value("userDb"))
            .andExpect(jsonPath("$.[2].resources.[0].resourceType").value("database"))
            .andExpect(jsonPath("$.[2].resources.[1].key").value("orderDb"))
            .andExpect(jsonPath("$.[2].resources.[1].resourceType").value("database"))
            .andExpect(jsonPath("$.[2].resources.[*].key").value(hasSize(2)))

            .andExpect(jsonPath("$.[*].key").value(hasSize(3)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/flow/spec/steps?stepType=ACTION"))
            .andDo(print())
            .andExpect(jsonPath("$.[0].key").value("actionkey"))
            .andExpect(jsonPath("$.[0].type").value("ACTION"))
            .andExpect(jsonPath("$.[1].key").value("otheraction"))
            .andExpect(jsonPath("$.[1].type").value("ACTION"))
            .andExpect(jsonPath("$.[*].key").value(hasSize(2)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/flow/spec/steps?stepType=CONDITION"))
            .andDo(print())
            .andExpect(jsonPath("$.[0].key").value("othercondition"))
            .andExpect(jsonPath("$.[0].type").value("CONDITION"))
            .andExpect(jsonPath("$.[*].key").value(hasSize(1)))
            .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    public void testResourceTypes() {
        tenantResourceTypeService.onRefresh("/config/tenants/TEST/testApp/resource-types/database.yml", loadFile("resource-types/database.yml"));
        tenantResourceTypeService.onRefresh("/config/tenants/TEST/testApp/resource-types/ole.yml", loadFile("resource-types/ole.yml"));

        mockMvc.perform(get("/api/flow/spec/resource-types"))
            .andDo(print())
            .andExpect(jsonPath("$.[0].key").value("excel"))
            .andExpect(jsonPath("$.[1].key").value("jdbc"))
            .andExpect(jsonPath("$.[2].key").value("word"))
            .andExpect(jsonPath("$.[*].key").value(hasSize(3)))
            .andExpect(status().isOk());

        tenantResourceTypeService.onRefresh("/config/tenants/TEST/testApp/resource-types/database.yml", "");
        tenantResourceTypeService.onRefresh("/config/tenants/TEST/testApp/resource-types/ole.yml", "");
    }

    @Test
    @SneakyThrows
    public void testCreateResource() {
        doAnswer(invocation -> {
            List<Configuration> configurations = invocation.getArgument(0);
            configurations.forEach(it -> tenantResourceConfigService.onRefresh(it.getPath(), it.getContent()));
            return null;
        }).when(tenantConfigRepository).updateConfigurations(any());

        mockMvc.perform(post("/api/flow/resources")
            .contentType("application/json")
            .content(loadFile("resource1.json")))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.resource.type.not.found"));


        tenantResourceTypeService.onRefresh("/config/tenants/TEST/testApp/resource-types/database.yml", loadFile("resource-types/database.yml"));
        tenantResourceTypeService.onRefresh("/config/tenants/TEST/testApp/resource-types/ole.yml", loadFile("resource-types/ole.yml"));

        mockMvc.perform(post("/api/flow/resources")
                .contentType("application/json")
                .content(loadFile("resource1.json")))
            .andDo(print())
            .andExpect(status().isOk())
            ;

        mockMvc.perform(post("/api/flow/resources")
                .contentType("application/json")
                .content(loadFile("resource1.json")))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.resource.already.exists"));
        ;

        mockMvc.perform(put("/api/flow/resources")
                .contentType("application/json")
                .content(loadFile("resource2.json")))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.resource.not.found"));
        ;

        mockMvc.perform(post("/api/flow/resources")
                .contentType("application/json")
                .content(loadFile("resource2.json")))
            .andDo(print())
            .andExpect(status().isOk())
        ;

        mockMvc.perform(get("/api/flow/resources")
                .contentType("application/json"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].key").value("user_database"))
            .andExpect(jsonPath("$.[0].data.url").value("jdbc:postgresql://localhost:5432/userdb"))
            .andExpect(jsonPath("$.[0].data.username").value("user"))
            .andExpect(jsonPath("$.[1].key").value("order-database"))
            .andExpect(jsonPath("$.[1].data.url").value("path-to-excel"))
            .andExpect(jsonPath("$.[*].key").value(hasSize(2)));

        mockMvc.perform(put("/api/flow/resources")
                .contentType("application/json")
                .content(loadFile("resource3.json")))
            .andDo(print())
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/flow/resources")
                .contentType("application/json"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].key").value("user_database"))
            .andExpect(jsonPath("$.[0].data.url").value("jdbc:postgresql://localhost:5432/user_db"))
            .andExpect(jsonPath("$.[0].data.username").value("secretuser"))
            .andExpect(jsonPath("$.[1].key").value("order-database"))
            .andExpect(jsonPath("$.[1].data.url").value("path-to-excel"))
            .andExpect(jsonPath("$.[*].key").value(hasSize(2)));

        mockMvc.perform(get("/api/flow/resources?resourceType=excel")
                .contentType("application/json"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].key").value("order-database"))
            .andExpect(jsonPath("$.[0].data.url").value("path-to-excel"))
            .andExpect(jsonPath("$.[*].key").value(hasSize(1)));

    }

    @Test
    @SneakyThrows
    public void testResourceFromLep() {
        tenantResourceConfigService.onRefresh("/config/tenants/TEST/testApp/resources/user_database.yml", loadFile("resource1.yml"));

        lep.onRefresh("/config/tenants/TEST/testApp/lep/test/Test.groovy", "lepContext.resources.jdbc.account_database.data.username");

        try (var context = lepManagementService.beginThreadContext()) {
            assertEquals("secretuser", testLepService.test());
        }
    }

}
