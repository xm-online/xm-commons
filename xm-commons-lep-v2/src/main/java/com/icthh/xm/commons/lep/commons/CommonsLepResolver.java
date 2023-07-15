package com.icthh.xm.commons.lep.commons;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommonsLepResolver implements LepKeyResolver {

    @Override
    public String group(LepMethod method) {
        return method.getParameter("group", String.class);
    }

    @Override
    public List<String> segments(LepMethod method) {
        return List.of(method.getParameter("name", String.class));
    }
}
