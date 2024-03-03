package com.icthh.xm.commons.lep.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import javax.tools.JavaFileObject;

import java.util.Map;

import static com.google.testing.compile.Compiler.javac;
import static javax.tools.JavaFileObject.Kind.CLASS;
import static org.junit.Assert.assertEquals;

public class MapWrapperProcessorUnitTest {

    // language=java
    String javaSource = "package com.icthh.xm.commons.lep.processor;\n" +
        "\n" +
        "import com.icthh.xm.commons.lep.api.BaseLepContext;\n" +
        "import com.icthh.xm.commons.lep.processor.GroovyMap;\n" +
        "\n" +
        "@GroovyMap\n" +
        "public class LepContext extends BaseLepContext {\n" +
        "   public String publicField;" +
        "}\n";

    @Test
    @SneakyThrows
    public void testMapAnnotationProcessor() {
        var source = JavaFileObjects.forSourceString(
            "com.icthh.xm.commons.lep.processor.LepContext",
            javaSource
        );

        Compilation compilation = javac()
            .withProcessors(new GroovyMapWrapperProcessor())
            .compile(source);

        JavaFileObject groovyMapLepContextWrapperFileObject = compilation.generatedFiles().stream().filter(it -> it.getKind() == CLASS)
            .filter(it -> it.getName().equals("/CLASS_OUTPUT/com/icthh/xm/commons/GroovyMapLepContextWrapper.class"))
            .findFirst().get();

        JavaFileObject lepContextFileObject = compilation.generatedFiles().stream().filter(it -> it.getKind() == CLASS)
            .filter(it -> it.getName().equals("/CLASS_OUTPUT/com/icthh/xm/commons/lep/processor/LepContext.class"))
            .findFirst().get();

        MemoryClassLoader classLoader = new MemoryClassLoader(lepContextFileObject, groovyMapLepContextWrapperFileObject);
        Class<?> lepContextClass = classLoader.loadClass("com.icthh.xm.commons.lep.processor.LepContext");
        Class<?> generatedClass = classLoader.loadClass("com.icthh.xm.commons.GroovyMapLepContextWrapper");

        BaseLepContext lepContext = (BaseLepContext) lepContextClass.getConstructors()[0].newInstance();
        lepContext.addAdditionalContext("key", "value");
        lepContext.inArgs = "inArgsMockValue";

        BaseLepContext wrapper = (BaseLepContext) generatedClass.getConstructors()[0].newInstance(lepContext);
        Map<String, Object> mapLepContext = (Map<String, Object>) wrapper;

        assertEquals("value", mapLepContext.get("key"));
        assertEquals("inArgsMockValue", mapLepContext.get("inArgs"));
    }

    static class MemoryClassLoader extends ClassLoader {
        private final byte[] generatedBytes;
        private final byte[] lepContextBytes;

        @SneakyThrows
        public MemoryClassLoader(JavaFileObject lepContext, JavaFileObject groovyMapLepContextWrapper) {
            this.generatedBytes = IOUtils.toByteArray(groovyMapLepContextWrapper.openInputStream());
            this.lepContextBytes = IOUtils.toByteArray(lepContext.openInputStream());
            defineClass("com.icthh.xm.commons.lep.processor.LepContext", lepContextBytes, 0, lepContextBytes.length);
            defineClass("com.icthh.xm.commons.GroovyMapLepContextWrapper", generatedBytes, 0, generatedBytes.length);
        }

    }
}
