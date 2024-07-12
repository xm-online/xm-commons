package com.icthh.xm.commons.flow.context;

import com.icthh.xm.commons.flow.context.StepLepAdditionalContext.StepContext;
import com.icthh.xm.commons.flow.domain.Step;
import com.icthh.xm.commons.flow.engine.context.FlowExecutionContext;
import com.icthh.xm.commons.flow.service.CodeSnippetExecutor;
import com.icthh.xm.commons.lep.TargetProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContextField;
import com.icthh.xm.commons.lep.api.LepBaseKey;
import com.icthh.xm.commons.lep.api.LepEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.icthh.xm.commons.config.client.utils.Utils.nullSafeMap;
import static com.icthh.xm.commons.flow.context.StepLepAdditionalContext.StepLepAdditionalContextField.STEP;
import static com.icthh.xm.commons.flow.engine.StepExecutorService.ACTION;
import static com.icthh.xm.commons.flow.engine.StepExecutorService.CONDITION;
import static com.icthh.xm.commons.flow.engine.StepExecutorService.FLOW_STEP_GROUP;
import static com.icthh.xm.commons.flow.engine.StepExecutorService.STEP_CLASS_EXECUTOR;

@Component
@RequiredArgsConstructor
public class StepLepAdditionalContext implements LepAdditionalContext<StepContext> {

    private static final Set<String> STEP_LEP = Set.of(ACTION, CONDITION, STEP_CLASS_EXECUTOR);

    private final CodeSnippetExecutor snippetExecutor;

    @Override
    public String additionalContextKey() {
        return STEP;
    }

    @Override
    public StepContext additionalContextValue() {
        return null;
    }

    @Override
    public Optional<StepContext> additionalContextValue(BaseLepContext lepContext, LepEngine lepEngine, TargetProceedingLep lepMethod) {
        LepBaseKey lepBaseKey = lepMethod.getLepBaseKey();
        if (FLOW_STEP_GROUP.equals(lepBaseKey.getGroup()) && STEP_LEP.contains(lepBaseKey.getBaseKey())) {
            FlowExecutionContext context = lepMethod.getParameter("context", FlowExecutionContext.class);
            Step step = lepMethod.getParameter("step", Step.class);
            return Optional.of(new StepContext(context, step, snippetExecutor));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Class<StepLepAdditionalContextField> fieldAccessorInterface() {
        return StepLepAdditionalContextField.class;
    }

    public interface StepLepAdditionalContextField extends LepAdditionalContextField {
        String STEP = "step";
        default StepContext getStep() {
            return (StepContext)get(STEP);
        }
    }

    public static class StepContext {
        public final Object input;
        public final Object context;
        public final Map<String, Object> parameters;

        private final String flowKey;
        private final String stepKey;
        private final CodeSnippetExecutor snippetExecutor;

        public StepContext(FlowExecutionContext context, Step step, CodeSnippetExecutor snippetExecutor) {
            var input = context.getStepInput().get(step.getKey());
            this.input = input;
            this.context = input;
            this.flowKey = context.getFlowKey();
            this.stepKey = step.getKey();
            this.parameters = Map.copyOf(nullSafeMap(step.getParameters()));
            this.snippetExecutor = snippetExecutor;
        }

        public Object runSnippet(String snippetKey, BaseLepContext lepContext) {
            return snippetExecutor.runCodeSnippet(lepContext, List.of(flowKey, stepKey, snippetKey));
        }
    }
}
