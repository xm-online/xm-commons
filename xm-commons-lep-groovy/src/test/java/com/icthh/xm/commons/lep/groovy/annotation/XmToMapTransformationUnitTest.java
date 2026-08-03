package com.icthh.xm.commons.lep.groovy.annotation;

import com.icthh.xm.commons.lep.groovy.GroovyMapConstructorTypeAnnotations;
import groovy.lang.GroovyClassLoader;
import lombok.SneakyThrows;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XmToMapTransformationUnitTest {

    private GroovyClassLoader gcl;

    @BeforeEach
    void setUp() {
        gcl = new GroovyClassLoader();
    }

    @AfterEach
    void tearDown() throws Exception {
        gcl.close();
    }

    Class<?> compile(String source) {
        return gcl.parseClass(source);
    }

    @SneakyThrows
    Class<?> load(String className) {
        return gcl.loadClass(className);
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> toMap(Object target) {
        return (Map<String, Object>) InvokerHelper.invokeMethod(target, "toMap", null);
    }

    Object instance(Class<?> clazz) {
        return InvokerHelper.invokeConstructorOf(clazz, null);
    }

    @Test
    void simpleFieldsPutUnconditionallyIncludingNulls() {
        Class<?> clazz = compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmToMap
            @XmToMap
            class SimpleOut {
                String name = "n"
                Integer count
            }
            """);
        Map<String, Object> map = toMap(instance(clazz));
        assertEquals("n", map.get("name"));
        assertTrue(map.containsKey("count"));
        assertNull(map.get("count"));
    }

    @Test
    void timeUuidAndEnumSerializedOnlyWhenNotNull() {
        compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmToMap
            import java.time.Instant
            enum OutStatus {
                OK
                String value() { return "ok-value" }
            }
            @XmToMap
            class RichOut {
                Instant createdAt = Instant.parse("2026-08-02T10:15:30Z")
                UUID id
                OutStatus status = OutStatus.OK
                OutStatus emptyStatus
            }
            """);
        Map<String, Object> map = toMap(instance(load("RichOut")));
        assertEquals("2026-08-02T10:15:30Z", map.get("createdAt"));
        assertFalse(map.containsKey("id"), "null time/uuid fields are skipped");
        assertEquals("ok-value", map.get("status"));
        assertFalse(map.containsKey("emptyStatus"));
    }

    @Test
    void enumWithoutValueMethodUsesName() {
        compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmToMap
            enum PlainStatus { DONE }
            @XmToMap
            class PlainEnumOut {
                PlainStatus status = PlainStatus.DONE
            }
            """);
        assertEquals("DONE", toMap(instance(load("PlainEnumOut"))).get("status"));
    }

    @Test
    void nestedAnnotatedFieldAndCollectionsSerialized() {
        compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmToMap
            @XmToMap
            class OutChild {
                String title = "t"
            }
            @XmToMap
            class OutParent {
                OutChild child = new OutChild()
                List<OutChild> children = [new OutChild(), null]
                List<String> tags = ["a", null]
                List<OutChild> missing
                Map<String, OutChild> byKey = [k: new OutChild()]
            }
            """);
        Map<String, Object> map = toMap(instance(load("OutParent")));
        assertEquals(Map.of("title", "t"), map.get("child"));
        assertEquals(List.of(Map.of("title", "t")), map.get("children"));      // null items skipped
        assertEquals(Arrays.asList("a", null), map.get("tags"));               // simple items keep nulls
        assertTrue(map.containsKey("missing"));
        assertNull(map.get("missing"));                                        // null collection -> explicit null
        assertEquals(Map.of("k", Map.of("title", "t")), map.get("byKey"));
    }

    @Test
    void superToMapIsMerged() {
        compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmToMap
            @XmToMap
            class OutBase { String baseField = "b" }
            @XmToMap
            class OutDerived extends OutBase { String ownField = "o" }
            """);
        Map<String, Object> map = toMap(instance(load("OutDerived")));
        assertEquals("b", map.get("baseField"));
        assertEquals("o", map.get("ownField"));
    }

    @Test
    void xmMapConvertToClosureOverridesSerialization() {
        compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConvert
            import com.icthh.xm.commons.lep.groovy.annotation.XmToMap
            @XmToMap
            class ToConvertOut {
                @XmMapConvert(to = { 'x:' + it })
                String name = "n"
                @XmMapConvert(to = { 'never' })
                String missing
            }
            """);
        Map<String, Object> map = toMap(instance(load("ToConvertOut")));
        assertEquals("x:n", map.get("name"));
        assertFalse(map.containsKey("missing"), "to closure is not called for null fields");
    }

    @Test
    @SneakyThrows
    void constructorToMapRoundTrip() {
        compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            import com.icthh.xm.commons.lep.groovy.annotation.XmToMap
            import java.time.Instant
            @XmMapConstructor
            @XmToMap
            class RoundChild { String title }
            @XmMapConstructor
            @XmToMap
            class RoundTrip {
                String name
                Instant createdAt
                RoundChild child
                List<RoundChild> children
            }
            """);
        Class<?> clazz = load("RoundTrip");
        Map<String, Object> source = Map.of(
            "name", "n",
            "createdAt", "2026-08-02T10:15:30Z",
            "child", Map.of("title", "t1"),
            "children", List.of(Map.of("title", "t2")));
        Object dto = clazz.getConstructor(Map.class).newInstance(source);
        Map<String, Object> out = toMap(dto);
        assertEquals("n", out.get("name"));
        assertEquals("2026-08-02T10:15:30Z", out.get("createdAt"));
        assertEquals(Map.of("title", "t1"), out.get("child"));
        assertEquals(List.of(Map.of("title", "t2")), out.get("children"));
    }

    @Test
    void doesNotAccumulateSharedTypeAnnotations() {
        GroovyMapConstructorTypeAnnotations.clear();
        for (int i = 0; i < 10; i++) {
            compile("""
                import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
                import com.icthh.xm.commons.lep.groovy.annotation.XmToMap
                @XmMapConstructor
                @XmToMap
                class LeakProbe%d { String name }
                """.formatted(i));
        }
        assertEquals(0, GroovyMapConstructorTypeAnnotations.accumulatedCount(),
            "XmMapConstructor must not reproduce the @MapConstructor shared-node leak");
    }
}
