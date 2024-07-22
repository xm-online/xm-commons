package com.icthh.xm.commons.flow.context;

import com.icthh.xm.commons.flow.context.StepsLepAdditionalContext.StepsContext;
import com.icthh.xm.commons.flow.engine.context.FlowExecutionContext;
import com.icthh.xm.commons.lep.TargetProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContextField;
import com.icthh.xm.commons.lep.api.LepBaseKey;
import com.icthh.xm.commons.lep.api.LepEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.icthh.xm.commons.flow.context.StepsLepAdditionalContext.StepsLepAdditionalContextField.STEPS;
import static com.icthh.xm.commons.flow.engine.StepExecutorService.ACTION;
import static com.icthh.xm.commons.flow.engine.StepExecutorService.CONDITION;
import static com.icthh.xm.commons.flow.engine.StepExecutorService.FLOW_STEP_GROUP;
import static com.icthh.xm.commons.flow.engine.StepExecutorService.STEP_CLASS_EXECUTOR;

@Component
@RequiredArgsConstructor
public class StepsLepAdditionalContext implements LepAdditionalContext<StepsContext> {

    private static final Set<String> STEP_LEP = Set.of(ACTION, CONDITION, STEP_CLASS_EXECUTOR);

    @Override
    public String additionalContextKey() {
        return STEPS;
    }

    @Override
    public StepsContext additionalContextValue() {
        return null;
    }

    @Override
    public Optional<StepsContext> additionalContextValue(BaseLepContext lepContext, LepEngine lepEngine, TargetProceedingLep lepMethod) {
        LepBaseKey lepBaseKey = lepMethod.getLepBaseKey();
        if (FLOW_STEP_GROUP.equals(lepBaseKey.getGroup()) && STEP_LEP.contains(lepBaseKey.getBaseKey())) {
            FlowExecutionContext context = lepMethod.getParameter("context", FlowExecutionContext.class);
            return Optional.of(new StepsContext(context));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Class<StepsLepAdditionalContextField> fieldAccessorInterface() {
        return StepsLepAdditionalContextField.class;
    }

    public interface StepsLepAdditionalContextField extends LepAdditionalContextField {
        String STEPS = "steps";
        default StepsContext getSteps() {
            return (StepsContext)get(STEPS);
        }
    }

    public static class StepLog {
        public Object input;
        public Object output;
    }

    public static class StepsContext implements Map<String, StepLog> {
        public final Map<String, StepLog> delegate;

        public StepsContext(FlowExecutionContext context) {
            Set<String> keys = new HashSet<>();
            keys.addAll(context.getStepInput().keySet());
            keys.addAll(context.getStepOutput().keySet());

            Map<String, StepLog> steps = new HashMap<>();
            keys.forEach(key -> {
                StepLog stepLog = new StepLog();
                stepLog.input = context.getStepInput().get(key);
                stepLog.output = context.getStepOutput().get(key);
                steps.put(key, stepLog);
            });

            this.delegate = Map.copyOf(steps);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        @Override
        public StepLog get(Object key) {
            return delegate.get(key);
        }

        @Override
        public StepLog put(String key, StepLog value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public StepLog remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends String, ? extends StepLog> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public Set<String> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<StepLog> values() {
            return delegate.values();
        }

        @Override
        public Set<Entry<String, StepLog>> entrySet() {
            return delegate.entrySet();
        }

    }

}
