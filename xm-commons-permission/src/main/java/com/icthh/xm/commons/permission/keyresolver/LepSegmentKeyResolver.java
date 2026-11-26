package com.icthh.xm.commons.permission.keyresolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LepSegmentKeyResolver implements LepKeyResolver {

    @Override
    public List<String> segments(LepMethod method) {
        return List.of(method.getParameter("lepSegment", String.class));
    }
}
