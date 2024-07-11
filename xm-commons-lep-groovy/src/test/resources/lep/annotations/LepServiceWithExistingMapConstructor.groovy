package TEST.testApp.lep.commons

import com.icthh.xm.commons.lep.groovy.annotation.LepConstructor

@LepConstructor
class LepServiceWithExistingMapConstructor {
    private final String stringWithValueFirst;

    private String someValue;

    LepServiceWithExistingMapConstructor(Map<String, Object> lepContext) {
        // final field will be set by annotation
        someValue = "setted in constructor";
    }
}

