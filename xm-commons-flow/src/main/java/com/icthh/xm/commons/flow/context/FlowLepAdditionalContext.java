package com.icthh.xm.commons.flow.context;

import com.icthh.xm.commons.flow.engine.FlowExecutor;
import com.icthh.xm.commons.flow.engine.context.FlowExecutionContext;
import com.icthh.xm.commons.flow.context.FlowLepAdditionalContext.FlowContext;
import com.icthh.xm.commons.flow.service.FlowService;
import com.icthh.xm.commons.lep.TargetProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContextField;
import com.icthh.xm.commons.lep.api.LepBaseKey;
import com.icthh.xm.commons.lep.api.LepEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

import static com.icthh.xm.commons.flow.context.FlowLepAdditionalContext.FlowLepAdditionalContextField.FLOW;
import static com.icthh.xm.commons.flow.engine.StepExecutorService.ACTION;
import static com.icthh.xm.commons.flow.engine.StepExecutorService.CONDITION;
import static com.icthh.xm.commons.flow.engine.StepExecutorService.FLOW_STEP_GROUP;
import static com.icthh.xm.commons.flow.engine.StepExecutorService.STEP_CLASS_EXECUTOR;

@Component
@RequiredArgsConstructor
public class FlowLepAdditionalContext implements LepAdditionalContext<FlowContext> {

    private static final Set<String> STEP_LEP = Set.of(ACTION, CONDITION, STEP_CLASS_EXECUTOR);

    private final FlowService flowService;

    @Override
    public String additionalContextKey() {
        return FLOW;
    }

    @Override
    public FlowContext additionalContextValue() {
        return null;
    }

    @Override
    public Optional<FlowContext> additionalContextValue(BaseLepContext lepContext, LepEngine lepEngine, TargetProceedingLep lepMethod) {
        LepBaseKey lepBaseKey = lepMethod.getLepBaseKey();
        if (FLOW_STEP_GROUP.equals(lepBaseKey.getGroup()) && STEP_LEP.contains(lepBaseKey.getBaseKey())) {
            FlowExecutionContext context = lepMethod.getParameter("context", FlowExecutionContext.class);
            return Optional.of(new FlowContext(context.getInput(), flowService));
        } else {
            return Optional.of(new FlowContext(null, flowService));
        }
    }

    @Override
    public Class<FlowLepAdditionalContextField> fieldAccessorInterface() {
        return FlowLepAdditionalContextField.class;
    }

    public interface FlowLepAdditionalContextField extends LepAdditionalContextField {
        String FLOW = "flow";
        default FlowContext getFlow() {
            return (FlowContext)get(FLOW);
        }
    }

    public static class FlowContext {
        public final Object input;
        public final Object context;
        private final FlowService flowService;

        public FlowContext(Object input, FlowService flowService) {
            this.input = input;
            this.context = input;
            this.flowService = flowService;
        }

        public FlowExecutionContext executeFlow(String flowKey, Object input) {
            return flowService.runFlowInternal(flowKey, input);
        }
    }
}
