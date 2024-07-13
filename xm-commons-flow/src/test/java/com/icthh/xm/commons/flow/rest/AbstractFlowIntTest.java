package com.icthh.xm.commons.flow.rest;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.flow.context.FlowLepAdditionalContext;
import com.icthh.xm.commons.flow.context.StepLepAdditionalContext;
import com.icthh.xm.commons.flow.context.StepsLepAdditionalContext;
import com.icthh.xm.commons.flow.context.TenantResourceLepAdditionalContext;
import com.icthh.xm.commons.flow.engine.FlowExecutor;
import com.icthh.xm.commons.flow.engine.StepExecutorService;
import com.icthh.xm.commons.flow.service.CodeSnippetExecutor;
import com.icthh.xm.commons.flow.service.CodeSnippetService;
import com.icthh.xm.commons.flow.service.FlowConfigService;
import com.icthh.xm.commons.flow.service.FlowService;
import com.icthh.xm.commons.flow.service.TenantResourceConfigService;
import com.icthh.xm.commons.flow.service.TenantResourceService;
import com.icthh.xm.commons.flow.service.YamlConverter;
import com.icthh.xm.commons.flow.service.resolver.FlowKeyLepKeyResolver;
import com.icthh.xm.commons.flow.service.resolver.FlowTypeLepKeyResolver;
import com.icthh.xm.commons.flow.service.resolver.SnippetListLepKeyResolver;
import com.icthh.xm.commons.flow.service.resolver.StepKeyResolver;
import com.icthh.xm.commons.flow.service.resolver.TenantResourceTypeLepKeyResolver;
import com.icthh.xm.commons.flow.service.resolver.TriggerResolver;
import com.icthh.xm.commons.flow.service.trigger.TriggerProcessor;
import com.icthh.xm.commons.flow.spec.resource.TenantResourceTypeService;
import com.icthh.xm.commons.flow.spec.step.StepSpecService;
import com.icthh.xm.commons.flow.spec.trigger.TriggerTypeSpecService;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.i18n.spring.config.LocalizationMessageProperties;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;


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
        FlowResource.class,
        FlowService.class,
        CodeSnippetService.class,
        CodeSnippetExecutor.class,
        SnippetListLepKeyResolver.class,
        TriggerProcessor.class,
        FlowTypeLepKeyResolver.class,
        TriggerResolver.class,
        FlowKeyLepKeyResolver.class,
        TriggerTypeSpecService.class,
        YamlConverter.class,
        FlowConfigService.class,
        FlowExecutor.class,
        StepKeyResolver.class,
        StepExecutorService.class,
        StepLepAdditionalContext.class,
        StepsLepAdditionalContext.class,
        FlowLepAdditionalContext.class
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
    @MockBean
    TenantConfigRepository tenantConfigRepository;
    @Autowired
    List<RefreshableConfiguration> configurations;

    List<String> updateConfigs;

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
        lepManagementService.beginThreadContext();
        updateConfigs = new ArrayList<>();
    }

    @After
    public void after() {
        updateConfigs.forEach(c -> updateConfig(c, null));
        lepManagementService.endThreadContext();
    }

    public void updateConfiguration(String path, String content) {
        updateConfigs.add(path);
        updateConfig(path, content);
    }

    private void updateConfig(String path, String content) {
        configurations.stream().filter(c -> c.isListeningConfiguration(path)).forEach(c -> c.onRefresh(path, content));
    }

    @SneakyThrows
    public static String loadFile(String path) {
        try (InputStream cfgInputStream = new ClassPathResource(path).getInputStream()) {
            return IOUtils.toString(cfgInputStream, UTF_8);
        }
    }

    public void mockSendConfigToRefresh() {
        doAnswer(invocation -> {
            if (invocation.getArguments()[0] == null) {
                return null;
            }
            List<Configuration> configurations = invocation.getArgument(0);
            configurations.forEach(c -> updateConfiguration(c.getPath(), c.getContent()));
            return null;
        }).when(tenantConfigRepository).updateConfigurations(any());

    }

}
