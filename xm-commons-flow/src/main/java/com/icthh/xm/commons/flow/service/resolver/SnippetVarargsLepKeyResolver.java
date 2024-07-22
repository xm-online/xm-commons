package com.icthh.xm.commons.flow.service.resolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.Arrays.asList;

@Component
public class SnippetVarargsLepKeyResolver implements LepKeyResolver {
    @Override
    public List<String> segments(LepMethod method) {
        String[] snippetKeys = method.getParameter("snippetKeys", String[].class);
        return asList(snippetKeys);
    }
}
