package com.icthh.xm.commons.flow.service.resolver;

import com.icthh.xm.commons.flow.domain.Step;
import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StepKeyResolver implements LepKeyResolver {
    @Override
    public List<String> segments(LepMethod method) {
        String step = method.getParameter("step", Step.class).getTypeKey();
        return List.of(step);
    }
}
