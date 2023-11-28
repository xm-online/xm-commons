package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.LogicExtensionPoint;

/**
 * Service used for testing LEP.
 */
@LepService(group = "general")
public class TestLepService {

    @LogicExtensionPoint("ScriptWithAround")
    public String sayHello() {
        return "Hello from java!";
    }

}
