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

    public FlowExecutionContext execute(Flow flow, Object input) {

        FlowExecutionContext context = new FlowExecutionContext(flow.getKey());

        try {
            // to step map
            Map<String, Step> steps = flow.getSteps().stream().collect(toMap(Step::getKey, identity()));
            Step firstStep = steps.get(flow.getStartStep());
            context.setInput(input);

            Step currentStep = firstStep;
            String lastActionKey = null;

            while(currentStep != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Execute step: {} with input: {}", currentStep.getKey(), input);
                }
                lastActionKey = currentStep instanceof Action ? currentStep.getKey() : lastActionKey;

                context.getStepInput().put(currentStep.getKey(), input);
                Object result = executeStep(input, currentStep, context);
                context.getStepOutput().put(currentStep.getKey(), result);
                if (log.isDebugEnabled()) {
                    log.debug("Step: {} executed with result: {}", currentStep.getKey(), result);
                }

                input = result;

                String nextStep = currentStep.getNext(result);
                if (nextStep == null) {
                    context.setOutput(context.getStepOutput().get(lastActionKey));
                    return context;
                }
                currentStep = steps.get(nextStep);
                if (currentStep == null && isNotBlank(nextStep)) {
                    log.error("Step for key: {} not found", nextStep);
                }
            }

            context.setOutput(context.getStepOutput().get(lastActionKey));
            return context;
        } catch (Throwable e) {
            log.error("Error execute flow with error {} | executionContext: {}", flow.getKey(), context, e);
            throw e;
        }
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
