package com.icthh.xm.commons.metric.resolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GaugeMetricKeyResolver implements LepKeyResolver {
    @Override
    public List<String> segments(LepMethod method) {
        return List.of(method.getParameter("gaugeName", String.class));
    }
}
