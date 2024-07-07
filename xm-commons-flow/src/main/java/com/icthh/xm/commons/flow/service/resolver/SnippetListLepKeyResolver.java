package com.icthh.xm.commons.flow.service.resolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SnippetListLepKeyResolver implements LepKeyResolver {
    @Override
    public List<String> segments(LepMethod method) {
        return method.getParameter("snippetKeys", List.class);
    }
}
