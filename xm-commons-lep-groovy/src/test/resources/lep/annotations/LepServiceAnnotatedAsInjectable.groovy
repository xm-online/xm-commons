package TEST.testApp.lep.commons

import com.icthh.xm.commons.lep.groovy.annotation.LepInjectableService

@LepInjectableService
class LepServiceAnnotatedAsInjectable {
    private final String stringWithValueFirst;

    LepServiceAnnotatedAsInjectable(def lepContext) {
        this.stringWithValueFirst = lepContext.stringWithValueFirst;
    }
}
