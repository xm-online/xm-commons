package com.icthh.xm.commons.flow.service.resolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;

import java.util.List;

import static java.util.Arrays.asList;

public class SnippetVarargsLepKeyResolver implements LepKeyResolver {
    @Override
    public List<String> segments(LepMethod method) {
        String[] snippetKeys = method.getParameter("snippetKeys", String[].class);
        return asList(snippetKeys);
    }
}
