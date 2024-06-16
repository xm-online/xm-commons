package TEST.testApp.lep.commons

import TEST.testApp.lep.commons.LepServiceAnnotatedAsInjectable
import TEST.testApp.lep.commons.LepServiceInjectableNoArgConstructor
import TEST.testApp.lep.commons.LepServiceWithExistingNoArgConstructor
import com.icthh.xm.commons.lep.groovy.annotation.LepConstructor

@LepConstructor(useLepFactory = false)
class LepServiceAnnotatedWithLepConstructor {
    private final String stringWithValueFirst;
    private final LepServiceAnnotatedAsInjectable lepServiceAnnotatedAsInjectable
    private final LepServiceWithExistingNoArgConstructor lepServiceWithExistingNoArgConstructor
    private final LepServiceInjectableNoArgConstructor lepServiceInjectableNoArgConstructor
}

