abstract class  A {
    void method() {
        [1, 2, 3].forEach {
            blabla();
        }
    }

    private static String blabla() {
        println "blabla"
        return "blabla";
    }
}

class B extends A {
}

new B().method()
return "STATIC_METHOD_WORKS"
