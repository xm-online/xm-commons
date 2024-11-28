package com.icthh.xm.commons.lep.keyresolver;

import com.icthh.xm.commons.lep.impl.DefaultLepKey;
import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FunctionLepKeyResolverUnitTest {

    private static final String PARAM_FUNCTION_KEY = "functionKey";

    private LepKeyResolver resolver;
    private LepMethod lepMethod;

    @BeforeEach
    void setUp() {
        resolver = new FunctionLepKeyResolver();
        lepMethod = mock(LepMethod.class);
    }

    @Test
    void group_functionKeyContainingSlash() {
        String baseGroup = "baseGroup";
        String baseKey = "baseKey";
        String functionKey = "package/test/FUNCTION.PACKAGE-TEST";

        when(lepMethod.getLepBaseKey()).thenReturn(new DefaultLepKey(baseGroup, baseKey));
        when(lepMethod.getParameter(eq(PARAM_FUNCTION_KEY), eq(String.class))).thenReturn(functionKey);

        String result = resolver.group(lepMethod);

        assertEquals("baseGroup.package/test", result);
        verify(lepMethod).getParameter(PARAM_FUNCTION_KEY, String.class);
    }

    @Test
    void group_functionKeyWithoutSlash() {
        String baseGroup = "baseGroup";
        String baseKey = "baseKey";
        String functionKey = "LEP-CONTEXT-TEST";

        when(lepMethod.getLepBaseKey()).thenReturn(new DefaultLepKey(baseGroup, baseKey));
        when(lepMethod.getParameter(eq(PARAM_FUNCTION_KEY), eq(String.class))).thenReturn(functionKey);

        String result = resolver.group(lepMethod);

        assertEquals("baseGroup", result);
        verify(lepMethod).getParameter(PARAM_FUNCTION_KEY, String.class);
    }

    @Test
    void segments_validFunctionKey() {
        String functionKey = "package/test/FUNCTION.PACKAGE-TEST";

        when(lepMethod.getParameter(eq(PARAM_FUNCTION_KEY), eq(String.class))).thenReturn(functionKey);

        List<String> result = resolver.segments(lepMethod);

        assertEquals(List.of("FUNCTION.PACKAGE-TEST"), result);
        verify(lepMethod).getParameter(PARAM_FUNCTION_KEY, String.class);
    }

    @Test
    void segments_functionKeyWithoutSlash() {
        String functionKey = "LEP-CONTEXT-TEST";

        when(lepMethod.getParameter(eq(PARAM_FUNCTION_KEY), eq(String.class))).thenReturn(functionKey);

        List<String> result = resolver.segments(lepMethod);

        assertEquals(List.of(functionKey), result);
        verify(lepMethod).getParameter(PARAM_FUNCTION_KEY, String.class);
    }
}
