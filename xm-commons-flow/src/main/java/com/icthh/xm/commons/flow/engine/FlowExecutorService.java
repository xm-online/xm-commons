package com.icthh.xm.commons.flow.engine;

import com.google.common.collect.Lists;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.flow.domain.Action;
import com.icthh.xm.commons.flow.domain.Condition;
import com.icthh.xm.commons.flow.domain.Flow;
import com.icthh.xm.commons.flow.domain.Step;
import com.icthh.xm.commons.flow.engine.context.FlowExecutionContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.range;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowExecutorService {

    private final StepExecutorService stepExecutorService;

    public FlowExecutionContext execute(Flow flow, Object input) {
        assertStartStepExists(flow);
        FlowExecutionContext context = new FlowExecutionContext(flow.getKey(), input);
        Map<String, Step> steps = flow.getSteps().stream().collect(toMap(Step::getKey, identity()));
        String lastActionKey = null;
        try {

            Step currentStep = steps.get(flow.getStartStep());
            while(currentStep != null) {
                lastActionKey = currentStep instanceof Action ? currentStep.getKey() : lastActionKey;
                input = runStep(currentStep, input, context);
                currentStep = getNextStep(currentStep, input, steps);
            }

            context.setOutput(context.getStepOutput().get(lastActionKey));
            return context;
        } catch (Throwable e) {
            log.error("Error execute flow with error {} | executionContext: {}", flow.getKey(), context, e);
            throw e;
        }
    }

    private void assertStartStepExists(Flow flow) {
        boolean isExists = isNotBlank(flow.getStartStep()) && flow.getSteps().stream().anyMatch(step ->
            flow.getStartStep().equals(step.getKey())
        );

        if (!isExists) {
            throw new BusinessException("error.flow.start.step.not.found", "Start step with key " + flow.getStartStep() + " not found");
        }
    }

    private Object runStep(Step currentStep, Object input, FlowExecutionContext context) {
        StopWatch stopWatch = StopWatch.createStarted();
        log.debug("Execute step: {} with input: {}", currentStep.getKey(), input);

        context.getStepInput().put(currentStep.getKey(), input);
        Object result = executeStep(currentStep, input, context);
        context.getStepOutput().put(currentStep.getKey(), result);

        log.debug("Step: {} executed with result: {}, {}ms", currentStep.getKey(), result, stopWatch.getTime(MICROSECONDS));
        return result;
    }

    private Object executeStep(Step currentStep, Object input, FlowExecutionContext context) {
        // TODO refactor this to some pattern that follow open close principle
        if (currentStep instanceof Action) {
            Action action = (Action) currentStep;
            if (TRUE.equals(action.getIsIterable())) {
                return executeIterableAction(action, input, context);
            }
            return stepExecutorService.executeAction(input, action, context);
        } else if (currentStep instanceof Condition) {
            return stepExecutorService.executeCondition(input, (Condition) currentStep, context);
        } else {
            throw new IllegalArgumentException("Unsupported step type: " + currentStep.getClass());
        }
    }

    @SneakyThrows
    private Object executeIterableAction(Action action, Object input, FlowExecutionContext context) {
        List<Object> items = readActionArray(action, input);
        context.resetIteration();
        context.setIterationsInput(items);
        for (int i = 0; i < items.size(); i++) {
            Object iterationItem = items.get(i);
            context.setIteration(i);
            context.setIterationItem(iterationItem);

            Object result = stepExecutorService.executeAction(iterationItem, action, context);
            context.getIterationsOutput().add(result);
        }

        List<Object> output = context.getIterationsOutput();
        if (TRUE.equals(action.getRemoveNullOutputForIterableResult())) {
            output = output.stream().filter(Objects::nonNull).collect(toList());
        }

        context.resetIteration();
        return output;
    }

    private static List<Object> readActionArray(Action action, Object input) {
        try {
            Object value = JsonPath.read(input, action.getIterableJsonPath());
            if (value instanceof Number) {
                Number iterations = (Number) value;
                return range(0, iterations.intValue()).boxed().collect(toList());
            } else if (value instanceof List) {
                return (List<Object>) value;
            } else if (value.getClass().isArray()) {
                return Arrays.asList((Object[]) value);
            } else {
                Iterable<?> iterable = (Iterable<?>) value;
                return Lists.newArrayList(iterable);
            }
        } catch (JsonPathException e) {
            log.error("Error read from action {} by json path {}", action, action.getIterableJsonPath(), e);
            if (TRUE.equals(action.getSkipIterableJsonPathError())) {
                return List.of();
            }
            throw e;
        }
    }

    private Step getNextStep(Step currentStep, Object result, Map<String, Step> steps) {
        String nextStep = currentStep.getNext(result);
        currentStep = nextStep != null ? steps.get(nextStep): null;
        if (currentStep == null && isNotBlank(nextStep)) {
            log.error("Step for key: {} not found", nextStep);
        }
        return currentStep;
    }

}
