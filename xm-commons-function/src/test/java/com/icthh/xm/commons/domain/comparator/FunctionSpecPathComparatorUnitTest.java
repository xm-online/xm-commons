package com.icthh.xm.commons.domain.comparator;

import com.icthh.xm.commons.domain.spec.FunctionSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FunctionSpecPathComparatorUnitTest {

    private final FunctionSpecPathComparator comparator = FunctionSpecPathComparator.of();

    @Test
    void sort_nullPathsFirst() {
        FunctionSpec spec1 = mockFunctionSpec("/api/path2");
        FunctionSpec spec2 = mockFunctionSpec(null);
        FunctionSpec spec3 = mockFunctionSpec("/api/path1");
        FunctionSpec spec4 = mockFunctionSpec(null);
        List<FunctionSpec> specs = new ArrayList<>(List.of(spec4, spec3, spec2, spec1));

        specs.sort(comparator);

        assertEquals(spec4, specs.get(0));
        assertEquals(spec2, specs.get(1));
        assertEquals(spec3, specs.get(2));
        assertEquals(spec1, specs.get(3));
    }

    @Test
    void sort_patternLast() {
        FunctionSpec spec1 = mockFunctionSpec("/api/**");
        FunctionSpec spec2 = mockFunctionSpec("/api/path1");
        FunctionSpec spec3 = mockFunctionSpec("/api/path2");
        List<FunctionSpec> specs = new ArrayList<>(List.of(spec3, spec2, spec1));

        specs.sort(comparator);

        assertEquals(spec2, specs.get(0));
        assertEquals(spec3, specs.get(1));
        assertEquals(spec1, specs.get(2));
    }

    @Test
    void sort_organiseGenerisPatterns() {
        FunctionSpec spec1 = mockFunctionSpec("/api/**");
        FunctionSpec spec2 = mockFunctionSpec("/api/*/resource");
        FunctionSpec spec3 = mockFunctionSpec("/api/resource/**");
        FunctionSpec spec4 = mockFunctionSpec("/api/resource/path");
        List<FunctionSpec> specs = new ArrayList<>(List.of(spec4, spec3, spec2, spec1));

        specs.sort(comparator);

        assertEquals(spec2, specs.get(0));
        assertEquals(spec4, specs.get(1));
        assertEquals(spec3, specs.get(2));
        assertEquals(spec1, specs.get(3));
    }

    @Test
    void sort_alphabeticalOrder() {
        FunctionSpec spec1 = mockFunctionSpec("/api/pathA");
        FunctionSpec spec2 = mockFunctionSpec("/api/pathB");
        FunctionSpec spec3 = mockFunctionSpec("/api/pathC");
        List<FunctionSpec> specs = new ArrayList<>(List.of(spec3, spec1, spec2));

        specs.sort(comparator);

        assertEquals(spec1, specs.get(0));
        assertEquals(spec2, specs.get(1));
        assertEquals(spec3, specs.get(2));
    }

    @Test
    void sort_mixedConditions() {
        FunctionSpec spec1 = mockFunctionSpec(null);
        FunctionSpec spec2 = mockFunctionSpec("/api/resource");
        FunctionSpec spec3 = mockFunctionSpec("/api/**");
        FunctionSpec spec4 = mockFunctionSpec("/api/path1");
        FunctionSpec spec5 = mockFunctionSpec("/api/resource/**");
        FunctionSpec spec6 = mockFunctionSpec(null);
        FunctionSpec spec7 = mockFunctionSpec("/api/path2");
        FunctionSpec spec8 = mockFunctionSpec("/api/path2");
        FunctionSpec spec9 = mockFunctionSpec("/api/resource/path3");
        List<FunctionSpec> specs = new ArrayList<>(List.of(spec9, spec8, spec7, spec6, spec5, spec4, spec3, spec2, spec1));

        specs.sort(comparator);

        assertEquals(spec6, specs.get(0));
        assertEquals(spec1, specs.get(1));
        assertEquals(spec4, specs.get(2));
        assertEquals(spec8, specs.get(3));
        assertEquals(spec7, specs.get(4));
        assertEquals(spec2, specs.get(5));
        assertEquals(spec9, specs.get(6));
        assertEquals(spec5, specs.get(7));
        assertEquals(spec3, specs.get(8));
    }

    private FunctionSpec mockFunctionSpec(String path) {
        FunctionSpec mockFunctionSpec = mock(FunctionSpec.class);
        when(mockFunctionSpec.getPath()).thenReturn(path);
        return mockFunctionSpec;
    }
}
