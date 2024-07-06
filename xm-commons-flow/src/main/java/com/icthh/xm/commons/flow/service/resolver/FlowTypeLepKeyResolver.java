package com.icthh.xm.commons.flow.service.resolver;

import com.icthh.xm.commons.flow.domain.dto.Flow;
import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;

import java.util.List;

public class FlowTypeLepKeyResolver implements LepKeyResolver {
    @Override
    public List<String> segments(LepMethod method) {
        return List.of(method.getParameter("flow", Flow.class).getKey());
    }
}
