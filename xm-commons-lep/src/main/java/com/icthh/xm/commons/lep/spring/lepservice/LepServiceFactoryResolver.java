package com.icthh.xm.commons.lep.spring.lepservice;

import com.icthh.xm.commons.lep.AppendLepKeyResolver;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import org.springframework.stereotype.Component;

@Component
public class LepServiceFactoryResolver extends AppendLepKeyResolver {

    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        String translatedFuncKey = translateToLepConvention(getRequiredStrParam(method, "serviceClassName"));
        return new String[]{translatedFuncKey};
    }
}
