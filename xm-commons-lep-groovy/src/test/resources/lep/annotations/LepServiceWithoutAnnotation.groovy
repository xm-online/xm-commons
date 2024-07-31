package TEST.testApp.lep.commons

class LepServiceWithoutAnnotation {
    private final String stringWithValueFirst;

    LepServiceWithoutAnnotation(def lepContext) {
        this.stringWithValueFirst = lepContext.stringWithValueFirst;
    }
}
