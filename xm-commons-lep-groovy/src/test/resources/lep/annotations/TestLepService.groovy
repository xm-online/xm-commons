package TEST.testApp.lep.service

import TEST.testApp.lep.commons.LepServiceInjectableNoArgConstructor
import com.icthh.xm.commons.lep.BaseProceedingLep
import com.icthh.xm.commons.lep.groovy.annotation.LepConstructor
import com.icthh.xm.commons.lep.groovy.annotation.LepIgnoreInject
import com.icthh.xm.commons.lep.groovy.annotation.LepInject
import com.icthh.xm.commons.lep.spring.LepThreadHelper
import TEST.testApp.lep.commons.LepServiceAnnotatedAsInjectable
import TEST.testApp.lep.commons.LepServiceAnnotatedWithLepConstructor
import TEST.testApp.lep.commons.LepServiceWithExistingConstructor
import TEST.testApp.lep.commons.LepServiceWithExistingNoArgConstructor
import TEST.testApp.lep.commons.LepServiceWithoutAnnotation
//import TEST.testApp.lep.commons.LepServiceWithExistingMapConstructor
import groovy.transform.ToString

@ToString
@LepConstructor(useLepFactory = true)
class TestLepService {
    private final BaseProceedingLep lep;
    @LepInject
    private LepThreadHelper thread;
    private final String stringWithValueFirst;
    private final String stringWithValueSecond
    private final String mustBeFirstHere;
    private final String mustBeSecondHere;
    private final String firstMeetString = "this value will be overridden";
    @LepIgnoreInject
    private final String initedManually = "this value will not be overridden";
    @LepIgnoreInject
    private final String notInited;
    private final Integer notPresentInLepContextNoAnnotationNeeded = 5;
    private static final String staticFieldIgnoredByLepConstructor = 'value';

    private final LepServiceAnnotatedAsInjectable lepServiceAnnotatedAsInjectable
    private final LepServiceAnnotatedWithLepConstructor lepServiceAnnotatedWithLepConstructor
    private final LepServiceWithExistingConstructor lepServiceWithExistingConstructor
    private final LepServiceWithExistingNoArgConstructor lepServiceWithExistingNoArgConstructor
    private final LepServiceWithoutAnnotation lepServiceWithoutAnnotation
    private final LepServiceInjectableNoArgConstructor lepServiceInjectableNoArgConstructor
    //private final LepServiceWithExistingMapConstructor lepServiceWithExistingMapConstructor
}
