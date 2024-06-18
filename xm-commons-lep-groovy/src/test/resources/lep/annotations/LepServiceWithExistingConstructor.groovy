package TEST.testApp.lep.commons

import com.icthh.xm.commons.lep.groovy.annotation.LepConstructor

@LepConstructor
class LepServiceWithExistingConstructor {
    private final String stringWithValueFirst;

    private String someValue;

    LepServiceWithExistingConstructor(def lepContext) {
        // final field will be set by annotation
        someValue = "setted in constructor";
    }
}

