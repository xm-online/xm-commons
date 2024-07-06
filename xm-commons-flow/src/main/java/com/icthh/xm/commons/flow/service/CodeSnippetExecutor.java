package com.icthh.xm.commons.flow.service;

import com.icthh.xm.commons.flow.service.resolver.SnippetListLepKeyResolver;
import com.icthh.xm.commons.flow.service.resolver.SnippetVarargsLepKeyResolver;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@LepService(group = "flow.snippets")
public class CodeSnippetExecutor {
    @LogicExtensionPoint(value = "Snippet", resolver = SnippetVarargsLepKeyResolver.class)
    public Object runCodeSnippet(Object input, String ...snippetKeys) {
        return null;
    }

    @LogicExtensionPoint(value = "Snippet", resolver = SnippetListLepKeyResolver.class)
    public Object runCodeSnippet(Object input, List<String> snippetKeys) {
        return null;
    }
}
