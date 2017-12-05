package com.icthh.xm.commons.logging;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

import static org.junit.Assert.assertTrue;

import com.icthh.xm.commons.logging.util.AopAnnotationUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

/**
 *
 */
public class AopAnnotationUtilUnitTest {

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @Before
    public void before() {

        MockitoAnnotations.initMocks(this);

        when(joinPoint.getSignature()).thenReturn(signature);

    }

    @Test
    public void testAnnotationDefaults() throws NoSuchMethodException {

        Class<?> aClass = TestServiceOnMethod.class;

        when(signature.getMethod()).thenReturn(aClass.getMethod("methodWithDefaults", String.class));
        when(signature.getDeclaringType()).thenReturn(aClass);

        Optional<LoggingAspectConfig> conf = AopAnnotationUtils.getConfigAnnotation(joinPoint);

        System.out.println(conf);

        assertTrue(conf.isPresent());
        assertTrue(conf.get().inputDetails());
        assertFalse(conf.get().inputCollectionAware());
        assertArrayEquals(new String[]{}, conf.get().inputIncludeParams());
        assertArrayEquals(new String[]{}, conf.get().inputExcludeParams());
        assertTrue(conf.get().resultDetails());
        assertTrue(conf.get().resultCollectionAware());

    }

    @Test
    public void testAnnotationOnMethod() throws NoSuchMethodException {

        Class<?> aClass = TestServiceOnMethod.class;

        when(signature.getMethod()).thenReturn(aClass.getMethod("execute", String.class));
        when(signature.getDeclaringType()).thenReturn(aClass);

        Optional<LoggingAspectConfig> conf = AopAnnotationUtils.getConfigAnnotation(joinPoint);

        System.out.println(conf);

        assertTrue(conf.isPresent());
        assertFalse(conf.get().inputDetails());
        assertFalse(conf.get().inputCollectionAware());
        assertArrayEquals(new String[]{}, conf.get().inputIncludeParams());
        assertArrayEquals(new String[]{}, conf.get().inputExcludeParams());
        assertTrue(conf.get().resultDetails());
        assertFalse(conf.get().resultCollectionAware());

    }

    @Test
    public void testAnnotationOnClass() throws NoSuchMethodException {

        Class<?> aClass = TestServiceOnClass.class;
        when(signature.getMethod()).thenReturn(aClass.getMethod("execute", String.class));
        when(signature.getDeclaringType()).thenReturn(aClass);

        Optional<LoggingAspectConfig> conf = AopAnnotationUtils.getConfigAnnotation(joinPoint);

        System.out.println(conf);

        assertTrue(conf.isPresent());
        assertFalse(conf.get().inputCollectionAware());
        assertFalse(conf.get().inputDetails());
        assertFalse(conf.get().resultDetails());
    }

    @Test
    public void testAnnotationOnSuperMethod() throws NoSuchMethodException {

        Class<?> aClass = TestServiceOnMethodExt.class;

        when(signature.getMethod()).thenReturn(aClass.getMethod("execute", String.class));
        when(signature.getDeclaringType()).thenReturn(aClass);

        Optional<LoggingAspectConfig> conf = AopAnnotationUtils.getConfigAnnotation(joinPoint);

        System.out.println(conf);

        assertTrue(conf.isPresent());
        assertFalse(conf.get().inputCollectionAware());
        assertFalse(conf.get().inputDetails());
        assertTrue(conf.get().resultDetails());
    }

    @Test
    public void testIncludeOnClassLevel() throws NoSuchMethodException {

        Class<?> aClass = TestServiceIncludeOnClass.class;

        when(signature.getMethod()).thenReturn(aClass.getMethod("method1",
                                                                Object.class,
                                                                String.class,
                                                                int.class));
        when(signature.getDeclaringType()).thenReturn(aClass);

        Optional<LoggingAspectConfig> conf = AopAnnotationUtils.getConfigAnnotation(joinPoint);
        assertTrue(conf.isPresent());
        assertArrayEquals(ArrayUtils.toArray("param1", "param3"), conf.get().inputIncludeParams());
        assertArrayEquals(new String[]{}, conf.get().inputExcludeParams());

    }

    @Test
    public void testIncludeOnClassLevelNoArgs() throws NoSuchMethodException {

        Class<?> aClass = TestServiceIncludeOnClass.class;

        when(signature.getMethod()).thenReturn(aClass.getMethod("method2"));
        when(signature.getDeclaringType()).thenReturn(aClass);

        Optional<LoggingAspectConfig> conf = AopAnnotationUtils.getConfigAnnotation(joinPoint);
        assertTrue(conf.isPresent());
        assertArrayEquals(ArrayUtils.toArray("param1", "param3"), conf.get().inputIncludeParams());
        assertArrayEquals(new String[]{}, conf.get().inputExcludeParams());

    }

    static class TestServiceOnMethod {

        @LoggingAspectConfig(resultCollectionAware = false, inputDetails = false)
        public String execute(String request) {
            return request + " was processed with config on method";
        }

        @LoggingAspectConfig
        public String methodWithDefaults(String request) {
            return "processed: " + request;
        }

    }

    static class TestServiceOnMethodExt extends TestServiceOnMethod {

        @Override
        public String execute(String request) {
            return request + " was processed with config on overridden method";
        }

    }

    @LoggingAspectConfig(inputDetails = false, resultDetails = false)
    static class TestServiceOnClass {

        public String execute(String request) {
            return request + " was processed with config on class";
        }

    }

    @LoggingAspectConfig(inputIncludeParams = {"param1", "param3"})
    static class TestServiceIncludeOnClass {

        public String method1(Object param1, String param2, int param3) {
            return null;
        }

        public String method2() {
            return null;
        }

    }

}
