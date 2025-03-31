package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.LogicExtensionPoint;

import com.icthh.xm.commons.lep.api.UseAsLepContext;
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

    @LogicExtensionPoint("TestLepMethodWithInputObject")
    public Object testLepMethodObject(Map<String, Object> input) {
        return null;
    }

    @LogicExtensionPoint("TestWithReturnMap")
    public Map<String, Object> testWithReturnMap() {
        return Map.of();
    }

    @LogicExtensionPoint("TestUseAsLepContext")
    public Object testUseAsLepContext(@UseAsLepContext LepContext lepContext) {
        return "";
    }
}
