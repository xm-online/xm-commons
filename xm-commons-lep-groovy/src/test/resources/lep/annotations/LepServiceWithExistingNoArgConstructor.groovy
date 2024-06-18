package TEST.testApp.lep.commons

import com.icthh.xm.commons.lep.groovy.annotation.LepConstructor

@LepConstructor
class LepServiceWithExistingNoArgConstructor {
    private final String stringWithValueFirst;

    private String someValue;

    LepServiceWithExistingNoArgConstructor() {
        // final field will be set by annotation
        someValue = "setted in constructor";
    }
}

