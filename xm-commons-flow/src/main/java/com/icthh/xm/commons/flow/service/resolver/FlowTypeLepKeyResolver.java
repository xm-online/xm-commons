package com.icthh.xm.commons.flow.service.resolver;

import com.icthh.xm.commons.flow.domain.flow.Flow;
import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FlowTypeLepKeyResolver implements LepKeyResolver {
    @Override
    public List<String> segments(LepMethod method) {
        return List.of(method.getParameter("flow", Flow.class).getKey());
    }
}
