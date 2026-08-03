package com.icthh.xm.commons.lep.groovy.annotation;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class XmMapConstructorTransformationUnitTest {

    private GroovyClassLoader gcl;

    @BeforeEach
    void setUp() {
        gcl = new GroovyClassLoader();
    }

    @AfterEach
    void tearDown() throws Exception {
        gcl.close();
    }

    /** compiles all classes in the source, returns the first; siblings are loadable via gcl.loadClass */
    Class<?> compile(String source) {
        return gcl.parseClass(source);
    }

    Object newInstance(Class<?> clazz, Map<String, Object> map) throws Exception {
        return clazz.getConstructor(Map.class).newInstance(map);
    }

    Object prop(Object target, String name) {
        return InvokerHelper.getProperty(target, name);
    }

    @Test
    void annotatedClassCompiles() {
        Class<?> clazz = compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor
            class SmokeDto {
                String name
            }
            """);
        assertNotNull(clazz);
    }

    @Test
    void mapsSimpleFieldsAndIgnoresExtraKeys() throws Exception {
        Class<?> clazz = compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor
            class SimpleDto {
                String name
                Integer count
                Boolean active
            }
            """);
        Object dto = newInstance(clazz, Map.of(
            "name", "n1", "count", 5, "active", true, "unknownKey", "ignored"));
        assertEquals("n1", prop(dto, "name"));
        assertEquals(5, prop(dto, "count"));
        assertEquals(true, prop(dto, "active"));
    }

    @Test
    void nullMapAndNullValuesAreSafe() throws Exception {
        Class<?> clazz = compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor
            class NullableDto {
                String name = "default"
            }
            """);
        Object fromNullMap = newInstance(clazz, null);
        assertEquals("default", prop(fromNullMap, "name"));
        Map<String, Object> withNull = new HashMap<>();
        withNull.put("name", null);
        Object fromNullValue = newInstance(clazz, withNull);
        assertEquals("default", prop(fromNullValue, "name"));
    }

    @Test
    void excludedFieldsAreNotAssigned() throws Exception {
        Class<?> clazz = compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor(excludes = ['internal'])
            class ExcludesDto {
                String name
                String internal = "keep"
            }
            """);
        Object dto = newInstance(clazz, Map.of("name", "n", "internal", "overwrite"));
        assertEquals("n", prop(dto, "name"));
        assertEquals("keep", prop(dto, "internal"));
    }

    @Test
    void noArgConstructorKeptByDefaultAndRemovableByFlag() throws Exception {
        Class<?> withDefault = compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor
            class WithDefaultCtor { String name }
            """);
        assertNotNull(withDefault.getConstructor());

        Class<?> withoutDefault = compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor(noArgConstructor = false)
            class WithoutDefaultCtor { String name }
            """);
        assertThrows(NoSuchMethodException.class, withoutDefault::getConstructor);
    }

    @Test
    void parsesJavaTimeAndUuidFromString() throws Exception {
        Class<?> clazz = compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            import java.time.*
            @XmMapConstructor
            class TimeDto {
                Instant createdAt
                LocalDate day
                LocalTime time
                LocalDateTime dateTime
                Duration duration
                UUID id
            }
            """);
        Object dto = newInstance(clazz, Map.of(
            "createdAt", "2026-08-02T10:15:30Z",
            "day", "2026-08-02",
            "time", "10:15:30",
            "dateTime", "2026-08-02T10:15:30",
            "duration", "PT2H",
            "id", "123e4567-e89b-12d3-a456-426614174000"));
        assertEquals(Instant.parse("2026-08-02T10:15:30Z"), prop(dto, "createdAt"));
        assertEquals(LocalDate.parse("2026-08-02"), prop(dto, "day"));
        assertEquals(LocalTime.parse("10:15:30"), prop(dto, "time"));
        assertEquals(LocalDateTime.parse("2026-08-02T10:15:30"), prop(dto, "dateTime"));
        assertEquals(Duration.parse("PT2H"), prop(dto, "duration"));
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), prop(dto, "id"));
    }

    @Test
    void mapsEnumByValueOfWithoutThrowingOnUnknown() throws Exception {
        compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            enum Status { NEW, DONE }
            @XmMapConstructor
            class EnumDto {
                Status status
            }
            """);
        Class<?> dtoClass = gcl.loadClass("EnumDto");
        Object known = newInstance(dtoClass, Map.of("status", "DONE"));
        assertEquals("DONE", String.valueOf(prop(known, "status")));
        Object unknown = newInstance(dtoClass, Map.of("status", "NOT_A_STATUS"));
        assertNull(prop(unknown, "status"));
    }

    @Test
    void mapsEnumViaFromValueWhenDeclaredWithoutThrowing() throws Exception {
        compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            enum Grade {
                HIGH('h'), LOW('l')
                final String code
                Grade(String code) { this.code = code }
                static Grade fromValue(String v) {
                    def found = values().find { it.code == v }
                    if (found == null) throw new IllegalArgumentException(v)
                    return found
                }
            }
            @XmMapConstructor
            class GradeDto {
                Grade grade
            }
            """);
        Class<?> dtoClass = gcl.loadClass("GradeDto");
        Object known = newInstance(dtoClass, Map.of("grade", "h"));
        assertEquals("HIGH", String.valueOf(prop(known, "grade")));
        Object unknown = newInstance(dtoClass, Map.of("grade", "zzz"));
        assertNull(prop(unknown, "grade"));
    }

    @Test
    void mapsNestedAnnotatedClassSameCompilationUnit() throws Exception {
        compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor
            class ChildDto { String title }
            @XmMapConstructor
            class ParentDto {
                String name
                ChildDto child
            }
            """);
        Class<?> parentClass = gcl.loadClass("ParentDto");
        Object parent = newInstance(parentClass, Map.of("name", "p", "child", Map.of("title", "c")));
        Object child = prop(parent, "child");
        assertEquals("c", prop(child, "title"));
    }

    @Test
    void mapsNestedAnnotatedClassFromPrecompiledUnit() throws Exception {
        compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor
            class PrecompiledChild { String title }
            """);
        Class<?> parentClass = compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor
            class LateParent {
                PrecompiledChild child
            }
            """);
        Object parent = newInstance(parentClass, Map.of("child", Map.of("title", "c")));
        assertEquals("c", prop(prop(parent, "child"), "title"));
    }

    @Test
    void mapChildClassesFalseAssignsRawValue() throws Exception {
        compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor
            class RawChild { String title }
            @XmMapConstructor(mapChildClasses = false)
            class RawParent {
                Object child
            }
            """);
        Class<?> parentClass = gcl.loadClass("RawParent");
        Map<String, Object> childMap = Map.of("title", "c");
        Object parent = newInstance(parentClass, Map.of("child", childMap));
        assertEquals(childMap, prop(parent, "child"));
    }

    @Test
    void superMapConstructorIsChained() throws Exception {
        compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor
            class BaseDto { String baseField }
            @XmMapConstructor
            class DerivedDto extends BaseDto { String ownField }
            """);
        Class<?> derived = gcl.loadClass("DerivedDto");
        Object dto = newInstance(derived, Map.of("baseField", "b", "ownField", "o"));
        assertEquals("b", prop(dto, "baseField"));
        assertEquals("o", prop(dto, "ownField"));
    }

    @Test
    void mapsListAndSetOfAnnotatedType() throws Exception {
        compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor
            class Item { String sku }
            @XmMapConstructor
            class OrderDto {
                List<Item> items
                Set<Item> uniqueItems
            }
            """);
        Class<?> orderClass = gcl.loadClass("OrderDto");
        List<Object> itemMaps = Arrays.asList(Map.of("sku", "s1"), null, Map.of("sku", "s2"));
        Object order = newInstance(orderClass, Map.of("items", itemMaps, "uniqueItems", List.of(Map.of("sku", "s3"))));
        List<?> items = (List<?>) prop(order, "items");
        assertEquals(2, items.size());
        assertEquals("s1", prop(items.get(0), "sku"));
        assertEquals("s2", prop(items.get(1), "sku"));
        Set<?> unique = (Set<?>) prop(order, "uniqueItems");
        assertEquals(1, unique.size());
    }

    @Test
    void mapsListOfJavaTimeStrings() throws Exception {
        Class<?> clazz = compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            import java.time.Instant
            @XmMapConstructor
            class TimesDto {
                List<Instant> moments
            }
            """);
        Object dto = newInstance(clazz, Map.of("moments", List.of("2026-08-02T10:15:30Z")));
        List<?> moments = (List<?>) prop(dto, "moments");
        assertEquals(Instant.parse("2026-08-02T10:15:30Z"), moments.get(0));
    }

    @Test
    void mapsMapValuesOfAnnotatedType() throws Exception {
        compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor
            class Entry { String label }
            @XmMapConstructor
            class RegistryDto {
                Map<String, Entry> entries
            }
            """);
        Class<?> registryClass = gcl.loadClass("RegistryDto");
        Object registry = newInstance(registryClass, Map.of("entries", Map.of("k1", Map.of("label", "l1"))));
        Map<?, ?> entries = (Map<?, ?>) prop(registry, "entries");
        assertEquals("l1", prop(entries.get("k1"), "label"));
    }

    @Test
    void mapCollectionsFalseAssignsRawCollections() throws Exception {
        compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor
            class RawCollItem { String sku }
            @XmMapConstructor(mapCollections = false)
            class RawCollOrder {
                List<RawCollItem> items
            }
            """);
        Class<?> orderClass = gcl.loadClass("RawCollOrder");
        List<Object> raw = List.of(Map.of("sku", "s1"));
        Object order = newInstance(orderClass, Map.of("items", raw));
        assertEquals(raw, prop(order, "items"));
    }

    @Test
    void untypedAndSimpleCollectionsPassThrough() throws Exception {
        Class<?> clazz = compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor
            class PlainListsDto {
                List plain
                List<String> strings
            }
            """);
        Object dto = newInstance(clazz, Map.of(
            "plain", List.of("a"),
            "strings", List.of("b")));
        assertEquals(List.of("a"), prop(dto, "plain"));
        assertEquals(List.of("b"), prop(dto, "strings"));
    }

    @Test
    void preAndPostClosuresRunInOrder() throws Exception {
        Class<?> clazz = compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor(
                pre = { order.add('pre:' + name) },
                post = { order.add('post:' + name) })
            class HookedDto {
                List<String> order = []
                String name
            }
            """);
        Object dto = newInstance(clazz, Map.of("name", "n"));
        assertEquals(List.of("pre:null", "post:n"), prop(dto, "order"));
    }

    @Test
    void postRunsEvenForNullMapAndCanRemapEnum() throws Exception {
        compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            enum Level { HIGH, LOW }
            @XmMapConstructor(post = {
                if (level == null && map != null) {
                    level = 'strong' == map.get('level') ? Level.HIGH : Level.LOW
                }
            })
            class LevelDto {
                Level level
            }
            """);
        Class<?> dtoClass = gcl.loadClass("LevelDto");
        Object remapped = newInstance(dtoClass, Map.of("level", "strong"));
        assertEquals("HIGH", String.valueOf(prop(remapped, "level")));
        Object standard = newInstance(dtoClass, Map.of("level", "LOW"));
        assertEquals("LOW", String.valueOf(prop(standard, "level")));
        newInstance(dtoClass, null); // post with null map must not throw
    }

    @Test
    void runtimeAnnotationStillReadableAfterClosureExtraction() throws Exception {
        Class<?> clazz = compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor(pre = { }, post = { })
            class HookAnnotatedDto { String name }
            """);
        assertNotNull(clazz.getAnnotation(XmMapConstructor.class));
    }

    @Test
    void xmMapConvertFromClosureOverridesMapping() throws Exception {
        Class<?> clazz = compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConvert
            @XmMapConstructor
            class FromConvertDto {
                @XmMapConvert(from = { ((String) it).toUpperCase() })
                String name
                String plain
            }
            """);
        Object dto = newInstance(clazz, Map.of("name", "abc", "plain", "p"));
        assertEquals("ABC", prop(dto, "name"));
        assertEquals("p", prop(dto, "plain"));

        Map<String, Object> withNull = new HashMap<>();
        withNull.put("name", null);
        Object nullDto = newInstance(clazz, withNull);
        assertNull(prop(nullDto, "name"), "from closure is not called for null values");
    }

    @Test
    void xmMapConvertFromAndToRoundTrip() throws Exception {
        compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConvert
            import com.icthh.xm.commons.lep.groovy.annotation.XmToMap
            @XmMapConstructor
            @XmToMap
            class BothConvertDto {
                @XmMapConvert(from = { ((String) it).toUpperCase() }, to = { ((String) it).toLowerCase() })
                String code
            }
            """);
        Class<?> clazz = gcl.loadClass("BothConvertDto");
        Object dto = newInstance(clazz, Map.of("code", "ab"));
        assertEquals("AB", prop(dto, "code"));
        @SuppressWarnings("unchecked")
        Map<String, Object> out = (Map<String, Object>) InvokerHelper.invokeMethod(dto, "toMap", null);
        assertEquals("ab", out.get("code"));
    }

    @Test
    void finalFieldsAreAssignedFromMap() throws Exception {
        Class<?> clazz = compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor
            class FinalDto {
                final String name
                final Integer count
                String mutable
            }
            """);
        Object dto = newInstance(clazz, Map.of("name", "n", "count", 7, "mutable", "m"));
        assertEquals("n", prop(dto, "name"));
        assertEquals(7, prop(dto, "count"));
        assertEquals("m", prop(dto, "mutable"));

        Object nullDto = newInstance(clazz, null);
        assertNull(prop(nullDto, "name"));
    }

    @Test
    void classWithFinalFieldsGetsNoDefaultConstructor() throws Exception {
        Class<?> clazz = compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor
            class FinalNoDefaultDto {
                final String name
            }
            """);
        assertThrows(NoSuchMethodException.class, clazz::getConstructor);
    }

    @Test
    void staticAndDollarFieldsAreSkipped() throws Exception {
        Class<?> clazz = compile("""
            import com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructor
            @XmMapConstructor
            class StaticFieldDto {
                static String shared = "s"
                String name
            }
            """);
        Object dto = newInstance(clazz, Map.of("shared", "x", "name", "n"));
        assertEquals("s", prop(dto, "shared"));
        assertEquals("n", prop(dto, "name"));
    }
}
