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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.Boolean.TRUE;

@Component
@LepService(group = "flow.step")
@RequiredArgsConstructor
public class StepExecutorService {

    private final StepSpecService stepSpecService;

    private StepExecutorService self;

    @LogicExtensionPoint(value = "Action", resolver = StepKeyResolver.class)
    public Object executeAction(Object input, Action step, FlowExecutionContext context) {
        return self.executeStepByClassImpl(stepSpecService.getStepSpec(step.getTypeKey()), input, step, context);
    }

    @LogicExtensionPoint(value = "Condition", resolver = StepKeyResolver.class)
    public Boolean executeCondition(Object input, Condition step, FlowExecutionContext context) {
        return TRUE.equals(self.executeStepByClassImpl(stepSpecService.getStepSpec(step.getTypeKey()), input, step, context));
    }

    @LogicExtensionPoint(value = "StepClassExecutor")
    public Object executeStepByClassImpl(StepSpec stepSpec, Object input, Step step, FlowExecutionContext context) {
        throw new NotImplementedException("Error resolve step. Pls check support default groovy lep.");
    }

    @Autowired
    public void setSelf(StepExecutorService self) {
        this.self = self;
    }

}
