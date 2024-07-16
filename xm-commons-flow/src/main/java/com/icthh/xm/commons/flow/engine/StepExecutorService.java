package com.icthh.xm.commons.flow.engine;

import com.icthh.xm.commons.flow.domain.Action;
import com.icthh.xm.commons.flow.domain.Condition;
import com.icthh.xm.commons.flow.domain.Step;
import com.icthh.xm.commons.flow.engine.context.FlowExecutionContext;
import com.icthh.xm.commons.flow.service.resolver.StepKeyResolver;
import com.icthh.xm.commons.flow.spec.step.StepSpec;
import com.icthh.xm.commons.flow.spec.step.StepSpecService;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.Boolean.TRUE;

@Component
@LepService(group = StepExecutorService.FLOW_STEP_GROUP)
@RequiredArgsConstructor
public class StepExecutorService {

    public static final String ACTION = "Action";
    public static final String CONDITION = "Condition";
    public static final String STEP_CLASS_EXECUTOR = "StepClassExecutor";
    public static final String FLOW_STEP_GROUP = "flow.step";

    private final StepSpecService stepSpecService;

    private StepExecutorService self;

    @LogicExtensionPoint(value = ACTION, resolver = StepKeyResolver.class)
    public Object executeAction(Object input, Action step, FlowExecutionContext context) {
        return self.executeStepByClassImpl(stepSpecService.getStepSpec(step.getTypeKey()), input, step, context);
    }

    @LogicExtensionPoint(value = CONDITION, resolver = StepKeyResolver.class)
    public Boolean executeCondition(Object input, Condition step, FlowExecutionContext context) {
        return TRUE.equals(self.executeStepByClassImpl(stepSpecService.getStepSpec(step.getTypeKey()), input, step, context));
    }

    @LogicExtensionPoint(value = STEP_CLASS_EXECUTOR)
    public Object executeStepByClassImpl(StepSpec stepSpec, Object input, Step step, FlowExecutionContext context) {
        if (StringUtils.isBlank(stepSpec.getImplementation())) {
            throw new IllegalArgumentException("Step implementation is not defined for step: " + step.getTypeKey());
        }
        throw new NotImplementedException("Error resolve step. Pls check support default groovy lep.");
    }

    @Autowired
    public void setSelf(StepExecutorService self) {
        this.self = self;
    }

}
