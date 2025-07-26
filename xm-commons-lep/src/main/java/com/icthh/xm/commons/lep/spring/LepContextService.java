package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.TargetProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.lep.api.LepMethod;
import java.util.Map;

public interface LepContextService {
    BaseLepContext createLepContext(LepEngine lepEngine, TargetProceedingLep lepMethod);
    BaseLepContext customize(BaseLepContext lepContext, LepEngine lepEngine, TargetProceedingLep lepMethod);
    Map<String, Object> buildInArgsMap(LepMethod lepMethod);
}
