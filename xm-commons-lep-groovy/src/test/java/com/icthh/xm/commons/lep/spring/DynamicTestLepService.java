package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.LogicExtensionPoint;

import com.icthh.xm.commons.lep.api.UseAsLepContext;
import java.util.Map;
import lombok.Data;

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

    @LogicExtensionPoint(value = "LepMethodWithArgExpression", resolverExpression = "#input.data.value")
    public String testLepMethodWithArgExpression(TestInput input) {
        return "Hello I am service method!";
    }

    @LogicExtensionPoint(value = "LepMethodWithExpression", resolverExpression = "input.data.value")
    public String testLepMethodWithExpression(TestInput input) {
        return "Hello I am service method!";
    }

    @LogicExtensionPoint(value = "LepMethodWithListExpression", resolverExpression = "{input.data.value, secondInput}")
    public String testLepMethodWithListExpression(TestInput input, String secondInput) {
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

    @Data public static class TestInput { private TestInputData data; }
    @Data public static class TestInputData { private String value; }
}
