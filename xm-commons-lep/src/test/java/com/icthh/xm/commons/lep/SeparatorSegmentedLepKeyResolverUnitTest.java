package com.icthh.xm.commons.lep;

import static com.icthh.xm.commons.lep.SeparatorSegmentedLepKeyResolver.translateToLepConvention;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.lep.api.LepKey;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.MethodSignature;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * The {@link SeparatorSegmentedLepKeyResolverUnitTest} class.
 */
public class SeparatorSegmentedLepKeyResolverUnitTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void translateToLepConventionExceptionOnNullKey() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("xmEntitySpecKey can't be null");

        translateToLepConvention(null);
    }

    @Test
    public void translateToLepConventionExceptionOnSuccess() {
        assertEquals("", translateToLepConvention(""));
        assertEquals("abc", translateToLepConvention("abc"));
        assertEquals("a_b_c", translateToLepConvention("a-b-c"));
        assertEquals("$a$b$c", translateToLepConvention(".a.b.c"));
        assertEquals("a$b__c$d_x$y$z_", translateToLepConvention("a.b--c.d-x.y.z-"));
    }

    @Test
    public void callsResolveKey() {
        SeparatorSegmentedLepKey baseKey = new SeparatorSegmentedLepKey("a.b.c", ".");

        SeparatorSegmentedLepKeyResolver resolver = mock(SeparatorSegmentedLepKeyResolver.class);
        when(resolver.resolveKey(eq(baseKey), any(), any())).thenReturn(baseKey);

        LepKey resolvedKey = resolver.resolve(baseKey, null, null);
        assertEquals(baseKey, resolvedKey);
    }

    @Test
    public void whenKeyIsNotSeparatorSegmentedLepKeyThenThrowException() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage(Matchers.startsWith("Unsupported base key type:"));

        LepKey baseKey = mock(LepKey.class);

        SeparatorSegmentedLepKeyResolver resolver = mock(SeparatorSegmentedLepKeyResolver.class);
        resolver.resolve(baseKey, null, null);
    }

    @Test
    public void getParamValueOnNullNameThrowException() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("paramName can't be null");

        SeparatorSegmentedLepKeyResolver resolver = new AsIsSeparatorSegmentedLepKeyResolver();
        resolver.getParamValue(null, null);
    }

    @Test
    public void getParamValueOnEmptyNameThrowException() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("paramName can't be blank");

        SeparatorSegmentedLepKeyResolver resolver = new AsIsSeparatorSegmentedLepKeyResolver();
        resolver.getParamValue(null, "");
    }

    @Test
    public void getMethodParamValue() {
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getParameterNames()).thenReturn(new String[] {"a", "b"});

        LepMethod lepMethod = mock(LepMethod.class);
        when(lepMethod.getMethodSignature()).thenReturn(signature);
        when(lepMethod.getMethodArgValues()).thenReturn(new Object[] {null, "valueB"});

        SeparatorSegmentedLepKeyResolver resolver = new AsIsSeparatorSegmentedLepKeyResolver();
        assertEquals("valueB", resolver.getParamValue(lepMethod, "b"));
    }

    @Test
    public void getMethodUndefinedParam() {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage(Matchers.startsWith("Can't find parameter 'c' for method:"));

        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getParameterNames()).thenReturn(new String[] {"a", "b"});

        LepMethod lepMethod = mock(LepMethod.class);
        when(lepMethod.getMethodSignature()).thenReturn(signature);

        SeparatorSegmentedLepKeyResolver resolver = new AsIsSeparatorSegmentedLepKeyResolver();
        resolver.getParamValue(lepMethod, "c");
    }

    @Test
    public void getMethodParamValueWithCast() {
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getParameterNames()).thenReturn(new String[] {"a"});

        LepMethod lepMethod = mock(LepMethod.class);
        when(lepMethod.getMethodSignature()).thenReturn(signature);
        when(lepMethod.getMethodArgValues()).thenReturn(new Object[] {10L});

        SeparatorSegmentedLepKeyResolver resolver = new AsIsSeparatorSegmentedLepKeyResolver();
        assertEquals(Long.valueOf(10L), resolver.getParamValue(lepMethod, "a", Long.class));
    }

    @Test
    public void getRequiredNonNullMethodParamValue() {
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getParameterNames()).thenReturn(new String[] {"a", "b"});

        LepMethod lepMethod = mock(LepMethod.class);
        when(lepMethod.getMethodSignature()).thenReturn(signature);
        when(lepMethod.getMethodArgValues()).thenReturn(new Object[] {null, "valueB"});

        SeparatorSegmentedLepKeyResolver resolver = new AsIsSeparatorSegmentedLepKeyResolver();
        assertEquals("valueB", resolver.getRequiredParam(lepMethod, "b", String.class));
    }

    @Test
    public void getRequiredNullMethodParamValue() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage(Matchers.startsWith("LEP method "));

        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getParameterNames()).thenReturn(new String[] {"a", "b"});

        LepMethod lepMethod = mock(LepMethod.class);
        when(lepMethod.getMethodSignature()).thenReturn(signature);
        when(lepMethod.getMethodArgValues()).thenReturn(new Object[] {null, "valueB"});

        SeparatorSegmentedLepKeyResolver resolver = new AsIsSeparatorSegmentedLepKeyResolver();
        resolver.getRequiredParam(lepMethod, "a", String.class);
    }

    @Test
    public void getStrParam() {
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getParameterNames()).thenReturn(new String[] {"a", "b"});

        LepMethod lepMethod = mock(LepMethod.class);
        when(lepMethod.getMethodSignature()).thenReturn(signature);
        when(lepMethod.getMethodArgValues()).thenReturn(new Object[] {null, "valueB"});

        SeparatorSegmentedLepKeyResolver resolver = new AsIsSeparatorSegmentedLepKeyResolver();
        assertNull(resolver.getStrParam(lepMethod, "a"));
        assertEquals("valueB", resolver.getStrParam(lepMethod, "b"));
    }

    @Test
    public void getRequiredStrParam() {
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getParameterNames()).thenReturn(new String[] {"a", "b"});

        LepMethod lepMethod = mock(LepMethod.class);
        when(lepMethod.getMethodSignature()).thenReturn(signature);
        when(lepMethod.getMethodArgValues()).thenReturn(new Object[] {null, "valueB"});

        SeparatorSegmentedLepKeyResolver resolver = new AsIsSeparatorSegmentedLepKeyResolver();
        assertEquals("valueB", resolver.getRequiredStrParam(lepMethod, "b"));
    }

    private static class AsIsSeparatorSegmentedLepKeyResolver extends SeparatorSegmentedLepKeyResolver {

        @Override
        protected LepKey resolveKey(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
            return baseKey;
        }

    }

}
