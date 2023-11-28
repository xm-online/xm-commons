package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.groovy.GroovyFileParser;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GroovyFileParseUnitTest {

    @Test
    public void testDetectOutsideCode() {
        GroovyFileParser gfp = new GroovyFileParser();

        String testScript = "\n\n package TEST.testApp.lep.commons\n" +
            "\n\n class TestClass {\n TestClass(def lepContext) {} \n}\n" +
            "\n\n def med(def b){};\n" +
            "\n\n class TestClass2 {\n TestClass2(def lepContext) {} \n}\n";

        GroovyFileParser.GroovyFileMetadata scriptMetaData = gfp.getFileMetaData(testScript);
        assertTrue(scriptMetaData.isScript());

        String testClasses = "\n\n package TEST.testApp.lep.commons\n" +
            "\n\n import Test.testApp.commons.TestClass;\n" +
            "\n\n import Test.testApp.commons.TestClass.TEST;\n" +
            "\n\n interface TestAnnotation {\n \n}\n" +
            "\n\n /* not outside code */\n" +
            "\n @Foo(bar=\"two\")\n @Foo(bar=\"one\")" +
            "\n\n // not outside def code\n" +
            "\n @Foos({@Foo(bar=\"one\"), @Foo(bar=\"two\")})" +
            "\n\n // not outside def code\n" +
            "\n class TestClass3 {\n TestClass3(def lepContext) {} \n}\n" +
            "\n\n // not outside def code\n" +
            "\n @TestAnnotation(value = \"test\")" +
            "\n class TestClass2 {\n TestClass2(def lepContext) {} \n}\n";

        GroovyFileParser.GroovyFileMetadata classMetaData = gfp.getFileMetaData(testClasses);
        assertFalse(classMetaData.isScript());
    }

}
