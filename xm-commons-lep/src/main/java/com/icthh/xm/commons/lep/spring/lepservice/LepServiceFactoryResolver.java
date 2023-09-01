package com.icthh.xm.commons.lep.spring.lepservice;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;

import java.util.List;

public class LepServiceFactoryResolver implements LepKeyResolver {

    @Override
    public List<String> segments(LepMethod method) {
        return List.of(method.getParameter("serviceClassName", String.class));
    }
}
