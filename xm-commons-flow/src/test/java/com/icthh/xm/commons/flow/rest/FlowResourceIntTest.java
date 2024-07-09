package com.icthh.xm.commons.flow.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.flow.domain.flow.Action;
import com.icthh.xm.commons.flow.domain.flow.Flow;
import com.icthh.xm.commons.flow.domain.flow.Step;
import com.icthh.xm.commons.flow.domain.flow.Trigger;
import com.icthh.xm.commons.flow.service.FlowConfigService;
import com.icthh.xm.commons.flow.spec.step.StepSpec;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import lombok.SneakyThrows;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.flow.steps.StepsRefreshableConfigurationUnitTest.loadFile;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FlowResourceIntTest extends AbstractFlowIntTest {

    @Autowired
    TestLepService testLepService;

    @Autowired
    FlowConfigService flowConfigService;

    @Autowired
    XmLepScriptConfigServerResourceLoader lep;

    @Test
    @SneakyThrows
    public void testRefreshFlowConfig() {
        flowConfigService.onRefresh("/config/tenants/TEST/testApp/flow/my-flow.yml", loadFile("flow.yml"));
        Flow flow = flowConfigService.getFlow("my-flow");
        assertThat(flow, equalTo(mockFlow()));
    }

    @Test
    @SneakyThrows
    public void testFlowCrud() {
        doAnswer(invocation -> {
            if (invocation.getArguments()[0] == null) {
                return null;
            }
            List<Configuration> configurations = invocation.getArgument(0);
            configurations.forEach(it -> {
                if (flowConfigService.isListeningConfiguration(it.getPath())) {
                    flowConfigService.onRefresh(it.getPath(), it.getContent());
                }
            });
            return null;
        }).when(tenantConfigRepository).updateConfigurations(any());

        lep.onRefresh("/config/tenants/TEST/testApp/lep/flow/trigger/TriggerUpdated$$httpkey.groovy", loadFile("testlep/TriggerUpdated.groovy"));

        Flow flow = mockFlow();
        flow.setDescription("Init description");
        mockMvc.perform(post("/api/flow")
            .contentType("application/json")
            .content(toJson(flow)))
            .andDo(print())
            .andExpect(status().isOk())
        ;

        ArgumentCaptor<List<Configuration>> captor = ArgumentCaptor.forClass(List.class);
        verify(tenantConfigRepository).updateConfigurations(captor.capture());
        List<Configuration> value = new ArrayList<>(captor.getValue());
        System.out.println(value);

        assertEquals(8, value.size());
        assertEquals("/config/tenants/TEST/testApp/flow/my-flow.yml", value.get(0).getPath());
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        var actual = objectMapper.readValue(value.get(0).getContent(), Map.class);
        var expected = objectMapper.readValue(loadFile("flow.yml"), Map.class);
        value.remove(0);

        assertEquals(expected, actual);
        value.sort(Comparator.comparing(Configuration::getPath));

        assertEquals("/config/tenants/TEST/test/otherspec.yml", value.get(0).getPath());
        assertEquals("foofoofoo", value.get(0).getContent());
        assertEquals("/config/tenants/TEST/test/somespec.yml", value.get(1).getPath());
        assertEquals("blablabla", value.get(1).getContent());
        assertEquals("/config/tenants/TEST/testApp/flow/snippets/Snippet$$my-flow$$step2$$mapping.js", value.get(2).getPath());
        assertEquals("return context.get('orders').map(order => { return { id: order.id, name: order.name }; })", value.get(2).getContent());
        assertEquals("/config/tenants/TEST/testApp/flow/snippets/Snippet$$my-flow$$step2$$precheck.js", value.get(3).getPath());
        assertEquals("if (context.get('orders').length > 0) { return true; } else { return false; }", value.get(3).getContent());
        assertEquals("/config/tenants/TEST/testApp/flow/snippets/Snippet$$my-flow$$step3$$mapping.js", value.get(4).getPath());
        assertEquals("return context.get('users').map(user => { return { id: user.id, name: user.name }; })", value.get(4).getContent());
        assertEquals("/config/tenants/TEST/testApp/flow/snippets/Snippet$$my-flow$$step3$$postcheck.js", value.get(5).getPath());
        assertEquals("if (context.get('votes').length > 0) { return true; } else { return false; }", value.get(5).getContent());
        assertEquals("/config/tenants/TEST/testApp/flow/snippets/Snippet$$my-flow$$step3$$precheck.js", value.get(6).getPath());
        assertEquals("if (context.get('users').length > 0) { return true; } else { return false; }", value.get(6).getContent());

        ResultActions flowGet = mockMvc.perform(get("/api/flow/my-flow"))
            .andDo(print())
            .andExpect(status().isOk());
        assertFlow(flowGet, "Init description");

        flow.setDescription("My flow description");

        mockMvc.perform(put("/api/flow")
                .contentType("application/json")
            .content(toJson(flow)))
            .andDo(print())
            .andExpect(status().isOk())
        ;

        ResultActions flowGet2 = mockMvc.perform(get("/api/flow/my-flow"))
            .andDo(print())
            .andExpect(status().isOk());
        assertFlow(flowGet2, "My flow description");

        mockMvc.perform(get("/api/flow"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].key").value("my-flow"))
        ;
    }

    private static void assertFlow(ResultActions flowGet, String description) throws Exception {
        flowGet
            .andExpect(jsonPath("$.key").value("my-flow"))
            .andExpect(jsonPath("$.steps").value(hasSize(3)))
            .andExpect(jsonPath("$.description").value(description))
            .andExpect(jsonPath("$.trigger.typeKey").value(equalTo("httpkey")))
            .andExpect(jsonPath("$.trigger.parameters.method").value(equalTo("GET")))
            .andExpect(jsonPath("$.trigger.parameters.url").value(equalTo("/api/orders")))
            .andExpect(jsonPath("$.steps[0].key").value(equalTo("step1")))
            .andExpect(jsonPath("$.steps[0].typeKey").value(equalTo("actionkey")))
            .andExpect(jsonPath("$.steps[0].parameters.query").value(equalTo("select * from orders")))
            .andExpect(jsonPath("$.steps[1].key").value(equalTo("step2")))
            .andExpect(jsonPath("$.steps[1].typeKey").value(equalTo("actionkey")))
            .andExpect(jsonPath("$.steps[1].parameters.query").value(equalTo("select * from users")))
            .andExpect(jsonPath("$.steps[1].snippets.precheck.content").value(equalTo("if (context.get('orders').length > 0) { return true; } else { return false; }")))
            .andExpect(jsonPath("$.steps[1].snippets.precheck.extension").value(equalTo("js")))
            .andExpect(jsonPath("$.steps[1].snippets.mapping.content").value(equalTo("return context.get('orders').map(order => { return { id: order.id, name: order.name }; })")))
            .andExpect(jsonPath("$.steps[1].snippets.mapping.extension").value(equalTo("js")))
            .andExpect(jsonPath("$.steps[1].snippets.postcheck.content").doesNotExist())
            .andExpect(jsonPath("$.steps[2].key").value(equalTo("step3")))
            .andExpect(jsonPath("$.steps[2].typeKey").value(equalTo("actionkey")))
            .andExpect(jsonPath("$.steps[2].parameters.query").value(equalTo("select * from votes")))
            .andExpect(jsonPath("$.steps[2].snippets.precheck.content").value(equalTo("if (context.get('users').length > 0) { return true; } else { return false; }")))
            .andExpect(jsonPath("$.steps[2].snippets.precheck.extension").value(equalTo("js")))
            .andExpect(jsonPath("$.steps[2].snippets.mapping.content").value(equalTo("return context.get('users').map(user => { return { id: user.id, name: user.name }; })")))
            .andExpect(jsonPath("$.steps[2].snippets.mapping.extension").value(equalTo("js")))
            .andExpect(jsonPath("$.steps[2].snippets.postcheck.content").value(equalTo("if (context.get('votes').length > 0) { return true; } else { return false; }")))
            .andExpect(jsonPath("$.steps[2].snippets.postcheck.extension").value(equalTo("js")));
        ;
    }

    @SneakyThrows
    public static String toJson(Object object) {
        return new ObjectMapper().writeValueAsString(object);
    }

    public Flow mockFlow() {
        Action step1 = new Action();
        step1.setKey("step1");
        step1.setTypeKey("actionkey");
        step1.setParameters(Map.of("query", "select * from orders"));
        step1.setType(StepSpec.StepType.ACTION);
        step1.setNext(List.of("step2"));

        // Snippets for Step 2
        Step.Snippet precheckSnippetStep2 = new Step.Snippet();
        precheckSnippetStep2.setContent("if (context.get('orders').length > 0) { return true; } else { return false; }");
        precheckSnippetStep2.setExtension("js");

        Step.Snippet mappingSnippetStep2 = new Step.Snippet();
        mappingSnippetStep2.setContent("return context.get('orders').map(order => { return { id: order.id, name: order.name }; })");
        mappingSnippetStep2.setExtension("js");

        // Step 2
        Action step2 = new Action();
        step2.setKey("step2");
        step2.setTypeKey("actionkey");
        step2.setParameters(Map.of("query", "select * from users"));
        step2.setSnippets(Map.of("precheck", precheckSnippetStep2, "mapping", mappingSnippetStep2));
        step2.setType(StepSpec.StepType.ACTION);
        step2.setNext(List.of("step3"));

        // Snippets for Step 3
        Step.Snippet precheckSnippetStep3 = new Step.Snippet();
        precheckSnippetStep3.setContent("if (context.get('users').length > 0) { return true; } else { return false; }");
        precheckSnippetStep3.setExtension("js");

        Step.Snippet mappingSnippetStep3 = new Step.Snippet();
        mappingSnippetStep3.setContent("return context.get('users').map(user => { return { id: user.id, name: user.name }; })");
        mappingSnippetStep3.setExtension("js");

        Step.Snippet postcheckSnippetStep3 = new Step.Snippet();
        postcheckSnippetStep3.setContent("if (context.get('votes').length > 0) { return true; } else { return false; }");
        postcheckSnippetStep3.setExtension("js");

        // Step 3
        Step step3 = new Action();
        step3.setKey("step3");
        step3.setTypeKey("actionkey");
        step3.setParameters(Map.of("query", "select * from votes"));
        step3.setSnippets(Map.of("precheck", precheckSnippetStep3, "mapping", mappingSnippetStep3, "postcheck", postcheckSnippetStep3));
        step3.setType(StepSpec.StepType.ACTION);

        // Trigger
        Trigger trigger = new Trigger();
        trigger.setTypeKey("httpkey");
        trigger.setParameters(Map.of("method", "GET", "url", "/api/orders"));

        // Flow
        Flow flow = new Flow();
        flow.setKey("my-flow");
        flow.setTrigger(trigger);
        flow.setSteps(List.of(step1, step2, step3));

        return flow;
    }

}
