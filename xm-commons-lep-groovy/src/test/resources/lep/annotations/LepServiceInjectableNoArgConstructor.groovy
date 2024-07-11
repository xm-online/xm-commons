package TEST.testApp.lep.commons


import com.icthh.xm.commons.lep.groovy.annotation.LepInjectableService

@LepInjectableService
class LepServiceInjectableNoArgConstructor {

    private String someValue;

    LepServiceInjectableNoArgConstructor() {
        // final field will be set by annotation
        someValue = "setted in constructor";
    }
}

