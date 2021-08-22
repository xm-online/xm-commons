package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.LogicExtensionPoint;

import java.util.Map;

/**
 * Service used for testing LEP.
 */
@LepService(group = "service")
public class DynamicTestLepService {

    @LogicExtensionPoint("TestLepMethod")
    public String testLepMethod() {
        return "Hello I am service method!";
    }

    @LogicExtensionPoint("TestLepMethodWithInput")
    public String testLepMethod(Map<String, Object> input) {
        return "Hello I am service method!";
    }
}
