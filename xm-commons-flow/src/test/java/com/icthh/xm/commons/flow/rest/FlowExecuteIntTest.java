package com.icthh.xm.commons.flow.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.flow.domain.Flow;
import com.icthh.xm.commons.flow.engine.FlowExecutor;
import com.icthh.xm.commons.flow.engine.context.FlowExecutionContext;
import com.icthh.xm.commons.flow.service.FlowConfigService;
import com.icthh.xm.commons.flow.service.FlowConfigService.FlowsConfig;
import com.icthh.xm.commons.flow.service.FlowService;
import lombok.SneakyThrows;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class FlowExecuteIntTest extends AbstractFlowIntTest {

    @Autowired
    TestLepService testLepService;

    @Autowired
    FlowConfigService flowConfigService;

    @Autowired
    FlowService flowService;

    @Autowired
    FlowExecutor flowExecutor;

    @Test
    @SneakyThrows
    public void testSimpleExecution() {
        String simpleFlow = loadFile("test-flow-execute/simple-flow.yml");
        mockSendConfigToRefresh();
        updateConfiguration("/config/tenants/TEST/testApp/lep/flow/step/Action$$sum.groovy",
            "return lepContext.flow.input.b + lepContext.step.parameters.a");
        updateConfiguration("/config/tenants/TEST/testApp/lep/flow/step/Condition$$groovyCondition.groovy",
            "return lepContext.step.runSnippet('groovyCondition', lepContext)");
        updateConfiguration("/config/tenants/commons/lep/flow/conditions/NotIsZeroCondition.groovy",
            loadFile("test-flow-execute/NotIsZeroCondition.groovy"));
        updateConfiguration("/config/tenants/commons/lep/flow/actions/DivideAction.groovy",
            loadFile("test-flow-execute/DivideAction.groovy"));

        updateConfiguration("/config/tenants/TEST/testApp/lep/flow/step/Action$$groovyAction.groovy",
            "return lepContext.step.runSnippet('groovyAction', lepContext)");
        updateConfiguration("/config/tenants/TEST/testApp/lep/flow/step/Action$$minus.groovy",
            "return (int) (lepContext.step.parameters.a - lepContext.flow.input.b)");

        updateConfiguration("/config/tenants/TEST/testApp/flow/step-spec/steps.yml", loadFile("test-flow-execute/steps.yml"));

        FlowsConfig flows = new ObjectMapper(new YAMLFactory()).readValue(simpleFlow, FlowsConfig.class);
        Flow flow = flows.getFlows().get(0);
        flowService.createFlow(flow);

        FlowExecutionContext executionContext = flowExecutor.execute(flow, Map.of("b", -1));
        assertEquals(0, executionContext.getOutput());
        assertEquals(
            mockContext(
                "simple-flow",
                Map.of("b", -1),
                0,
                Map.of("step1", Map.of("b", -1), "step2", 0, "step3", false),
                Map.of("step1", 0, "step2", false, "step3", false)
            ),
            executionContext
        );

        executionContext = flowExecutor.execute(flow, Map.of("b", 2));
        assertEquals(8, executionContext.getOutput());
        assertEquals(
            mockContext(
                "simple-flow",
                Map.of("b", 2),
                8,
                Map.of("step1", Map.of("b", 2), "step2", 3, "step3", false, "step4", true),
                Map.of("step1", 3, "step2", false, "step3", true, "step4", 8)
            ),
            executionContext
        );

        executionContext = flowExecutor.execute(flow, Map.of("b", 5));
        assertEquals(2, executionContext.getOutput());
        assertEquals(
            mockContext(
                "simple-flow",
                Map.of("b", 5),
                2,
                Map.of("step1", Map.of("b", 5), "step2", 6, "step5", true),
                Map.of("step1", 6, "step2", true, "step5", 2)
            ),
            executionContext
        );
    }

    private FlowExecutionContext mockContext(
        String key,
                             Map<String, Object> input,
                             Object output,
                             Map<String, Object> stepInput,
                             Map<String, Object> stepOutput
    ) {
        var context = new FlowExecutionContext(key);
        context.setInput(input);
        context.setOutput(output);
        for (Map.Entry<String, Object> entry : stepInput.entrySet()) {
            context.getStepInput().put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Object> entry : stepOutput.entrySet()) {
            context.getStepOutput().put(entry.getKey(), entry.getValue());
        }
        return context;
    }

}
