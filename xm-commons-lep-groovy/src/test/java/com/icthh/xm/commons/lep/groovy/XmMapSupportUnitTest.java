package com.icthh.xm.commons.lep.groovy;

import groovy.lang.Closure;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class XmMapSupportUnitTest {

    /** groovy would pass a real closure; an anonymous Closure behaves identically for the helper */
    private static Closure<Object> closure(java.util.function.Function<Object, Object> fn) {
        return new Closure<>(null) {
            @Override
            public Object call(Object... args) {
                return fn.apply(args[0]);
            }
        };
    }

    private enum Color { RED, GREEN }

    @Test
    void mapListMapsItemsAndSkipsNulls() {
        List<Object> result = XmMapSupport.mapList(java.util.Arrays.asList("a", null, "b"), closure(v -> v + "!"));
        assertEquals(List.of("a!", "b!"), result);
    }

    @Test
    void mapListReturnsNullForNull() {
        assertNull(XmMapSupport.mapList(null, closure(v -> v)));
    }

    @Test
    void mapSetMapsItemsAndSkipsNulls() {
        Set<Object> result = XmMapSupport.mapSet(java.util.Arrays.asList("a", null), closure(v -> v + "!"));
        assertEquals(Set.of("a!"), result);
    }

    @Test
    void mapMapMapsValuesKeepsKeysAndNullValues() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("x", "a");
        source.put("y", null);
        Map<Object, Object> result = XmMapSupport.mapMap(source, closure(v -> v + "!"));
        assertEquals("a!", result.get("x"));
        assertNull(result.get("y"));
        assertEquals(2, result.size());
    }

    @Test
    void mapMapReturnsNullForNull() {
        assertNull(XmMapSupport.mapMap(null, closure(v -> v)));
    }

    @Test
    void toEnumParsesKnownValue() {
        assertEquals(Color.RED, XmMapSupport.toEnum(Color.class, "RED"));
    }

    @Test
    void toEnumReturnsNullForUnknownValueInsteadOfThrowing() {
        assertNull(XmMapSupport.toEnum(Color.class, "MAGENTA"));
    }

    @Test
    void toEnumReturnsNullForNull() {
        assertNull(XmMapSupport.toEnum(Color.class, null));
    }

    @Test
    void safeEnumSwallowsRuntimeExceptions() {
        assertNull(XmMapSupport.safeEnum("BAD", closure(v -> { throw new IllegalArgumentException(); })));
        assertEquals(Color.GREEN, XmMapSupport.safeEnum("GREEN", closure(v -> Color.valueOf((String) v))));
        assertNull(XmMapSupport.safeEnum(null, closure(v -> Color.RED)));
    }

    @Test
    void toMapListIdentityKeepsNulls() {
        assertEquals(java.util.Arrays.asList("a", null), XmMapSupport.toMapList(java.util.Arrays.asList("a", null)));
        assertNull(XmMapSupport.toMapList(null));
    }

    @Test
    void toMapListWithMapperSkipsNulls() {
        assertEquals(List.of("a!"), XmMapSupport.toMapList(java.util.Arrays.asList("a", null), closure(v -> v + "!")));
        assertNull(XmMapSupport.toMapList(null, closure(v -> v)));
    }

    @Test
    void toMapMapMapsValuesKeepsNulls() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("x", "a");
        source.put("y", null);
        Map<Object, Object> result = XmMapSupport.toMapMap(source, closure(v -> v + "!"));
        assertEquals("a!", result.get("x"));
        assertNull(result.get("y"));
        assertNull(XmMapSupport.toMapMap(null, closure(v -> v)));
    }
}
