package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.processor.GroovyMap;

@GroovyMap
public class LepContext extends BaseLepContext {

    public String stringWithValueFirst = "First";
    public String stringWithValueSecond = "Second";
    public SubClassOfLepContext subClassOfLepContext = new SubClassOfLepContext();

    public static class SubClassOfLepContext {
        public StringBuilder fieldInSubClass = new StringBuilder("valueFromLepSubClass");
    }

}
