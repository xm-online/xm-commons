package com.icthh.xm.commons.lep.groovy;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.ast.ClassNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GroovyMapConstructorTypeAnnotationsUnitTest {

    private static final int GENERATIONS = 10;

    private static final String MODEL_SOURCE = """
        import groovy.transform.MapConstructor
        @MapConstructor
        class Model%d {
            Integer repeatCount = null
            String  interval    = null
        }
        """;

    /** a lone Map property is the special named argument case, which uses the other shared node */
    private static final String SINGLE_MAP_PROPERTY_SOURCE = """
        import groovy.transform.MapConstructor
        @MapConstructor
        class SingleMapProperty%d {
            Map options = null
        }
        """;

    /** an accumulated node is visible to every later compilation in this JVM, so do not leave one */
    @AfterEach
    void leaveTheSharedNodesEmpty() {
        GroovyMapConstructorTypeAnnotations.clear();
    }

    @Test
    void groovyAccumulatesOneTypeAnnotationPerCompiledMapConstructorClass() throws Exception {
        GroovyMapConstructorTypeAnnotations.clear();

        for (int i = 1; i <= GENERATIONS; i++) {
            compile(MODEL_SOURCE, i);
        }

        assertEquals(GENERATIONS, accumulated("MAP_TYPE").size(),
            "groovy is expected to leak one @NamedParams node per compiled @MapConstructor class");
    }

    @Test
    void clearDropsTheAccumulatedNodes() throws Exception {
        for (int i = 1; i <= GENERATIONS; i++) {
            compile(MODEL_SOURCE, i);
        }
        assertTrue(accumulated("MAP_TYPE").size() > 0);

        GroovyMapConstructorTypeAnnotations.clear();

        assertEquals(0, accumulated("MAP_TYPE").size());
    }

    /**
     * Pins down where the leak is not: a lone Map property routes through LHMAP_TYPE, and neither node
     * grows there. Only the ordinary case accumulates, on MAP_TYPE.
     */
    @Test
    void theSpecialNamedArgumentCaseAccumulatesOnNeitherNode() throws Exception {
        GroovyMapConstructorTypeAnnotations.clear();

        for (int i = 1; i <= GENERATIONS; i++) {
            compile(SINGLE_MAP_PROPERTY_SOURCE, i);
        }

        assertEquals(0, accumulated("MAP_TYPE").size());
        assertEquals(0, accumulated("LHMAP_TYPE").size());
    }

    /**
     * The reference is swapped, not the list emptied: lep classes compile lazily on request threads,
     * so a reader can be copying this list while the refresh thread resets it.
     */
    @Test
    void swapsTheListInsteadOfEmptyingItInPlace() throws Exception {
        for (int i = 1; i <= GENERATIONS; i++) {
            compile(MODEL_SOURCE, i);
        }
        List<?> before = accumulated("MAP_TYPE");
        assertTrue(before.size() > 0);

        GroovyMapConstructorTypeAnnotations.clear();

        assertSame(Collections.emptyList(), accumulated("MAP_TYPE"));
        assertEquals(GENERATIONS, before.size(), "the list a concurrent reader already held must stay whole");
    }

    @Test
    void compilationStillWorksAfterClearing() throws Exception {
        for (int i = 1; i <= GENERATIONS; i++) {
            compile(MODEL_SOURCE, i);
            GroovyMapConstructorTypeAnnotations.clear();
        }

        Class<?> model = compile(MODEL_SOURCE, GENERATIONS + 1);
        Object instance = model.getDeclaredConstructor(Map.class).newInstance(Map.of("repeatCount", 7));

        assertEquals(7, model.getMethod("getRepeatCount").invoke(instance));
    }

    private static Class<?> compile(String template, int index) throws Exception {
        try (GroovyClassLoader classLoader = new GroovyClassLoader()) {
            return classLoader.parseClass(template.formatted(index));
        }
    }

    /** the very list the production code resets, read the same lazily created way */
    private static List<?> accumulated(String sharedNodeField) throws Exception {
        Field nodeField = Class.forName("org.codehaus.groovy.transform.MapConstructorASTTransformation")
            .getDeclaredField(sharedNodeField);
        nodeField.setAccessible(true);
        Field typeAnnotations = ClassNode.class.getDeclaredField("typeAnnotations");
        typeAnnotations.setAccessible(true);
        List<?> value = (List<?>) typeAnnotations.get((ClassNode) nodeField.get(null));
        return value == null ? List.of() : value;
    }
}
