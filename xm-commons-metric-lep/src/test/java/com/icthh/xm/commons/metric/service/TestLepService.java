package com.icthh.xm.commons.metric.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;

/**
 * Service used for testing LEP in metrics module.
 */
@LepService(group = "services")
public class TestLepService {

    @LogicExtensionPoint("TestLep")
    public String testLep() {
        return "Hello from metrics test!";
    }

}
