package com.icthh.xm.commons.flow.engine;

import com.icthh.xm.commons.flow.domain.Action;
import com.icthh.xm.commons.flow.domain.Condition;
import com.icthh.xm.commons.flow.domain.Flow;
import com.icthh.xm.commons.flow.domain.Step;
import com.icthh.xm.commons.flow.engine.context.FlowExecutionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowExecutor {

    private final StepExecutorService stepExecutorService;

    public Object execute(Flow flow, Object input) {
        try {
            return executeInternal(flow, input);
        } catch (Throwable e) {
            log.error("Error execute flow with error {}", flow, e);
            throw e;
        }
    }

    private Object executeInternal(Flow flow, Object input) {
        // to step map
        Map<String, Step> steps = flow.getSteps().stream().collect(toMap(Step::getKey, identity()));
        Step firstStep = steps.get(flow.getStartStep());
        FlowExecutionContext context = new FlowExecutionContext();
        context.setInput(input);

        Step currentStep = firstStep;
        String lastActionKey = null;

        lastActionKey = currentStep instanceof Action ? currentStep.getKey() : lastActionKey;

        while(currentStep != null) {
            if (log.isTraceEnabled()) {
                log.trace("Execute step: {} with input: {}", currentStep.getKey(), input);
            }
            context.getStepInput().put(currentStep.getKey(), input);
            Object result = executeStep(input, currentStep, context);
            context.getStepOutput().put(currentStep.getKey(), result);
            if (log.isTraceEnabled()) {
                log.trace("Step: {} executed with result: {}", currentStep.getKey(), result);
            }

            String nextStep = currentStep.getNext(result);
            if (nextStep == null) {
                return context.getStepOutput().get(lastActionKey);
            }
            currentStep = steps.get(nextStep);
            if (currentStep == null && isNotBlank(nextStep)) {
                log.error("Step for key: {} not found", nextStep);
            }
        }

        return context.getStepOutput().get(lastActionKey);
    }

    private Object executeStep(Object input, Step currentStep, FlowExecutionContext context) {
        // TODO refactor this to some pattern that follow open close principle
        if (currentStep instanceof Action) {
            return stepExecutorService.executeAction(input, (Action) currentStep, context);
        } else if (currentStep instanceof Condition) {
            return stepExecutorService.executeCondition(input, (Condition) currentStep, context);
        } else {
            throw new IllegalArgumentException("Unsupported step type: " + currentStep.getClass());
        }
    }

}
