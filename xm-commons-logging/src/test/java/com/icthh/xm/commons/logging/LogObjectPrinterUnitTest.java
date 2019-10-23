package com.icthh.xm.commons.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Test for log object printer. Created by medved on 27.06.17.
 */
public class LogObjectPrinterUnitTest {

    @Mock
    JoinPoint joinPoint;

    @Mock
    MethodSignature ms;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(joinPoint.getSignature()).thenReturn(ms);
    }

    @Test
    public void testPrintException() {
        assertEquals("null", LogObjectPrinter.printException(null));
        assertEquals("java.lang.RuntimeException: Some error",
                     LogObjectPrinter.printException(new RuntimeException("Some error")));
    }

    @Test
    public void testPrintExceptionWithStacktraceInfo() {
        assertEquals("null", LogObjectPrinter.printExceptionWithStackInfo(null));
        assertTrue(StringUtils.contains(LogObjectPrinter.printExceptionWithStackInfo(new RuntimeException("Some "
                                                                                                          + "error")),
                                        "java.lang.RuntimeException: Some error com.icthh.xm.commons.logging."
                                        + "LogObjectPrinterUnitTest.testPrintExceptionWithStacktraceInfo"));
    }

    @Test
    public void testComposeUrl() {

        String[] arr1 = new String[]{"/first", "/second"};
        String[] arr2 = new String[]{"/third", "/forth"};

        assertEquals("", LogObjectPrinter.joinUrlPaths(null));
        assertEquals("", LogObjectPrinter.joinUrlPaths(null, (Object[]) null));

        assertEquals("/first/second", LogObjectPrinter.joinUrlPaths(arr1));
        assertEquals("/third/forth", LogObjectPrinter.joinUrlPaths(arr2));

        assertEquals("/first/second", LogObjectPrinter.joinUrlPaths(arr1, (Object[]) null));
        assertEquals("/third/forth", LogObjectPrinter.joinUrlPaths(null, arr2));

        Integer[] arrInt = new Integer[]{1, 2, 3};

        assertEquals(
            "printerror:java.lang.IllegalArgumentException: Cannot store java.lang.Integer in an array "
            + "of java.lang.String",
            LogObjectPrinter.joinUrlPaths(arr1, arrInt));

    }

    @Test
    public void testGetCallMethod() {

        assertEquals("?:?", LogObjectPrinter.getCallMethod(null));

        when(ms.getDeclaringType()).thenReturn(null);
        assertEquals("?:null", LogObjectPrinter.getCallMethod(joinPoint));

        when(ms.getDeclaringType()).thenReturn(String.class);
        when(ms.getName()).thenReturn("toString");
        assertEquals("String:toString", LogObjectPrinter.getCallMethod(joinPoint));

    }

    @Test
    public void testPrintInputParams() {

        when(ms.getParameterNames()).thenReturn(new String[]{"param1", "param2", "param3"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"val1", "val2", "val3"});

        assertEquals("joinPoint is null", LogObjectPrinter.printInputParams(null));
        assertEquals("param1=val1,param2=val2,param3=val3", LogObjectPrinter.printInputParams(joinPoint));

        when(ms.getParameterNames()).thenReturn(new String[]{"param1", "password", "param3"});
        assertEquals("param1=val1,password=*****,param3=val3", LogObjectPrinter.printInputParams(joinPoint));
        assertEquals("param3=val3,password=*****", LogObjectPrinter.printInputParams(joinPoint, "param3", "password"));
    }

    @Test
    public void testPrintCollectionAware() {
        assertEquals("null", LogObjectPrinter.printCollectionAware(null));
        assertEquals("string1", LogObjectPrinter.printCollectionAware("string1"));
        assertEquals("[<ArrayList> size = 5]", LogObjectPrinter.printCollectionAware(Arrays.asList(1, 2, 3, 4, 5)));
    }

    @Test
    public void testPrintInputParamsNoDetail() throws NoSuchMethodException {

        Class<?> aClass = TestServiceInput.class;
        Method method = aClass.getMethod("methodNoDetail", String.class, int.class);

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);
        when(ms.getParameterNames()).thenReturn(getParamNames(method));
        when(joinPoint.getArgs()).thenReturn(new Object[]{"value1", 42});

        assertEquals("joinPoint is null", LogObjectPrinter.printInputParams(null));
        assertEquals("#hidden#", LogObjectPrinter.printInputParams(joinPoint));

    }

    @Test
    public void testPrintInputParamsNoDefault() throws NoSuchMethodException {

        Class<?> aClass = TestServiceInput.class;
        Method method = aClass.getMethod("methodDefaultDetails", String.class, int.class, Double.class);

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);
        when(ms.getParameterNames()).thenReturn(new String[]{"param1", "param2", "param3"});

        when(joinPoint.getArgs()).thenReturn(new Object[]{"value1", 42, 35.5});

        assertEquals("param1=value1,param2=42,param3=35.5", LogObjectPrinter.printInputParams(joinPoint));

    }

    @Test
    public void testPrintInputParamsIncludeParams() throws NoSuchMethodException {

        Class<?> aClass = TestServiceInput.class;
        Method method = aClass.getMethod("methodIncludeParams", String.class, int.class, Double.class);

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);
        when(ms.getParameterNames()).thenReturn(new String[]{"param1", "param2", "param3"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"value1", 42, 35.5});

        assertEquals("param2=42,param3=35.5", LogObjectPrinter.printInputParams(joinPoint));

    }

    @Test
    public void testPrintInputParamsIncludeNonExistentParams() throws NoSuchMethodException {

        Class<?> aClass = TestServiceInput.class;
        Method method = aClass.getMethod("methodIncludeNonExistentParams", String.class, int.class, Double.class);

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);
        when(ms.getParameterNames()).thenReturn(new String[]{"param1", "param2", "param3"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"value1", 42, 35.5});

        assertEquals("param2=42", LogObjectPrinter.printInputParams(joinPoint));

    }

    @Test
    public void testPrintInputParamsExclude() throws NoSuchMethodException {

        Class<?> aClass = TestServiceInput.class;
        Method method = aClass.getMethod("methodExcludeParams", String.class, int.class, Double.class);

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);
        when(ms.getParameterNames()).thenReturn(new String[]{"param1", "param2", "param3"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"value1", 42, 35.5});

        assertEquals("param1=value1,param3=35.5", LogObjectPrinter.printInputParams(joinPoint));

    }

    @Test
    public void testPrintInputParamsIncludeAndExclude() throws NoSuchMethodException {

        Class<?> aClass = TestServiceInput.class;
        Method method = aClass.getMethod("methodIncludeAndExcludeParams", String.class, int.class, Double.class);

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);
        when(ms.getParameterNames()).thenReturn(new String[]{"param1", "param2", "param3"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"value1", 42, 35.5});

        assertEquals("param2=42,param3=35.5", LogObjectPrinter.printInputParams(joinPoint));

    }


    @Test
    public void testPrintInputCollectionDefault() throws NoSuchMethodException {

        Class<?> aClass = TestServiceInput.class;
        Method method = aClass.getMethod("methodInputCollectionDefault", String.class, List.class, Map.class);

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);
        when(ms.getParameterNames()).thenReturn(new String[]{"param1", "list", "map"});

        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("one", 1);
        map.put("two", 2);

        when(joinPoint.getArgs()).thenReturn(new Object[]{"value1", Arrays.asList(1, 2, 3), map});

        assertEquals("param1=value1,list=[1, 2, 3],map={one=1, two=2}", LogObjectPrinter.printInputParams(joinPoint));

    }

    @Test
    public void testPrintInputCollectionAware() throws NoSuchMethodException {

        Class<?> aClass = TestServiceInput.class;
        Method method = aClass.getMethod("methodInputCollectionAware", String.class, List.class, Map.class);

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);
        when(ms.getParameterNames()).thenReturn(new String[]{"param1", "list", "map"});

        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("one", 1);
        map.put("two", 2);

        when(joinPoint.getArgs()).thenReturn(new Object[]{"value1", Arrays.asList(1, 2, 3), map});

        assertEquals("param1=value1,list=[<ArrayList> size = 3],map={one=1, two=2}",
                     LogObjectPrinter.printInputParams(joinPoint));

    }

    @Test
    public void testPrintInputArray() throws NoSuchMethodException {

        Class<?> aClass = TestServiceInput.class;
        Method method = aClass.getMethod("methodInputArray", String[].class);

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);
        when(ms.getParameterNames()).thenReturn(new String[]{"values"});

        String[] values = new String[] {"a_one", "a_two", "a_three"};

        when(joinPoint.getArgs()).thenReturn(new Object[]{values});

        assertEquals("values=[a_one, a_two, a_three]",
                     LogObjectPrinter.printInputParams(joinPoint));

    }


    @Test
    public void testPrintInputArrayCollectionAware() throws NoSuchMethodException {

        Class<?> aClass = TestServiceInput.class;
        Method method = aClass.getMethod("methodInputArrayCollectionAware", String[].class);

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);
        when(ms.getParameterNames()).thenReturn(new String[]{"values"});

        String[] values = new String[] {"a_one", "a_two", "a_three"};

        when(joinPoint.getArgs()).thenReturn(new Object[]{values});

        assertEquals("values=[<String[]> length = 3]",
                     LogObjectPrinter.printInputParams(joinPoint));

    }

    @Test
    public void testPrintMethodConfigOverridesClassConfig() throws NoSuchMethodException {

        Class<?> aClass = TestServiceInput2.class;
        Method method = aClass.getMethod("methodIncludeParams", String.class, int.class, Double.class);

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);
        when(ms.getParameterNames()).thenReturn(new String[]{"param1", "param2", "param3"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"value1", 42, 35.5});

        assertEquals("param2=42,param3=35.5", LogObjectPrinter.printInputParams(joinPoint));

    }

    @Test
    public void testPrintMethodGetConfigFromClassLever() throws NoSuchMethodException {

        Class<?> aClass = TestServiceInput2.class;
        Method method = aClass.getMethod("methodDefaultDetails", String.class, int.class, Double.class);

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);
        when(ms.getParameterNames()).thenReturn(new String[]{"param1", "param2", "param3"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"value1", 42, 35.5});

        assertEquals("#hidden#", LogObjectPrinter.printInputParams(joinPoint));

    }


    @Test
    public void testPrintConfigOverridesParamsToPrint() throws NoSuchMethodException {

        Class<?> aClass = TestServiceInput2.class;
        Method method = aClass.getMethod("methodIncludeParams", String.class, int.class, Double.class);

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);
        when(ms.getParameterNames()).thenReturn(new String[]{"param1", "param2", "param3"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"value1", 42, 35.5});

        assertEquals("param2=42,param3=35.5", LogObjectPrinter.printInputParams(joinPoint, "param1"));

    }

    @Test
    public void testPrintParamsNoConfigToPrint() throws NoSuchMethodException {

        Class<?> aClass = TestServiceInput.class;
        Method method = aClass.getMethod("methodWithoutLogConfig", String.class, int.class, Double.class);

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);
        when(ms.getParameterNames()).thenReturn(new String[]{"param1", "param2", "param3"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"value1", 42, 35.5});

        //test LogObjectPrinter.printInputParams(joinPoint, "param1", "param3")
        assertEquals("param1=value1,param3=35.5", LogObjectPrinter.printInputParams(joinPoint, "param1", "param3"));

    }

    @Test
    public void testPrintParamsWithConfigToPrint() throws NoSuchMethodException {

        Class<?> aClass = TestServiceInput.class;
        Method method = aClass.getMethod("methodDefaultDetails", String.class, int.class, Double.class);

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);
        when(ms.getParameterNames()).thenReturn(new String[]{"param1", "param2", "param3"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"value1", 42, 35.5});

        //test LogObjectPrinter.printInputParams(joinPoint, "param1", "param3")
        assertEquals("param1=value1,param3=35.5", LogObjectPrinter.printInputParams(joinPoint, "param1", "param3"));

    }

    @Test
    public void testPrintResultNoConfig() throws NoSuchMethodException {

        Class<?> aClass = TestServiceResult.class;
        Method method = aClass.getMethod("methodReturnNoConfig");

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);

        assertEquals("null", LogObjectPrinter.printResult(null, null));
        assertEquals("null", LogObjectPrinter.printResult(joinPoint, null));
        assertEquals("String value", LogObjectPrinter.printResult(joinPoint, "String value"));
        assertEquals("[<ArrayList> size = 3]", LogObjectPrinter.printResult(joinPoint, Arrays.asList(1, 2, 3)));
        assertEquals("String value", LogObjectPrinter.printResult(joinPoint, "String value", true));
        assertEquals("#hidden#", LogObjectPrinter.printResult(joinPoint, "String value", false));

    }

    @Test
    public void testPrintResultDefaultConfig() throws NoSuchMethodException {

        Class<?> aClass = TestServiceResult.class;
        Method method = aClass.getMethod("methodReturnDefaultConfig");

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);

        assertEquals("null", LogObjectPrinter.printResult(null, null));
        assertEquals("null", LogObjectPrinter.printResult(joinPoint, null));
        assertEquals("String value", LogObjectPrinter.printResult(joinPoint, "String value"));
        assertEquals("[<ArrayList> size = 3]", LogObjectPrinter.printResult(joinPoint, Arrays.asList(1, 2, 3)));
        assertEquals("String value", LogObjectPrinter.printResult(joinPoint, "String value", true));
        // printResultDetail input parameter is overridden by @LoggingAspectConfig
        assertEquals("String value", LogObjectPrinter.printResult(joinPoint, "String value", false));

    }


    @Test
    public void testPrintResultDetailsSuppressed() throws NoSuchMethodException {

        Class<?> aClass = TestServiceResult.class;
        Method method = aClass.getMethod("methodReturnDetailsSuppressed");

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);

        assertEquals("null", LogObjectPrinter.printResult(null, null));
        assertEquals("#hidden#", LogObjectPrinter.printResult(joinPoint, null));
        assertEquals("#hidden#", LogObjectPrinter.printResult(joinPoint, "String value"));
        assertEquals("#hidden#", LogObjectPrinter.printResult(joinPoint, Arrays.asList(1, 2, 3)));
        assertEquals("#hidden#", LogObjectPrinter.printResult(joinPoint, "String value", true));
        assertEquals("#hidden#", LogObjectPrinter.printResult(joinPoint, "String value", false));

    }

    @Test
    public void testPrintResultPrintCollection() throws NoSuchMethodException {

        Class<?> aClass = TestServiceResult.class;
        Method method = aClass.getMethod("methodReturnPrintWholeCollection");

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);

        assertEquals("null", LogObjectPrinter.printResult(null, null));
        assertEquals("null", LogObjectPrinter.printResult(joinPoint, null));
        assertEquals("String value", LogObjectPrinter.printResult(joinPoint, "String value"));
        assertEquals("[1, 2, 3]", LogObjectPrinter.printResult(joinPoint, Arrays.asList(1, 2, 3)));
        assertEquals("String value", LogObjectPrinter.printResult(joinPoint, "String value", true));
        // printResultDetail input parameter is overridden by @LoggingAspectConfig
        assertEquals("String value", LogObjectPrinter.printResult(joinPoint, "String value", false));

    }

    // do not use this method for test where real parameter names should appear.
    // Because it will require -parameter compiler option
    private String[] getParamNames(Method method) {
        List<String> params = Arrays.stream(method.getParameters())
                                    .map(Parameter::getName)
                                    .collect(Collectors.toList());
        return params.toArray(new String[0]);
    }

    static class TestServiceInput {

        @LoggingAspectConfig(inputDetails = false)
        public void methodNoDetail(String param1, int param2) {
        }

        @LoggingAspectConfig
        public void methodDefaultDetails(String param1, int param2, Double param3) {
        }

        public void methodWithoutLogConfig(String param1, int param2, Double param3) {
        }

        @LoggingAspectConfig(inputIncludeParams = {"param2", "param3"})
        public void methodIncludeParams(String param1, int param2, Double param3) {
        }

        @LoggingAspectConfig(inputIncludeParams = {"param2", "paramNonExist"})
        public void methodIncludeNonExistentParams(String param1, int param2, Double param3) {
        }

        @LoggingAspectConfig(inputExcludeParams = {"param2"})
        public void methodExcludeParams(String param1, int param2, Double param3) {
        }

        @LoggingAspectConfig(inputIncludeParams = {"param2", "param3"}, inputExcludeParams = {"param2"})
        public void methodIncludeAndExcludeParams(String param1, int param2, Double param3) {
        }


        public void methodInputCollectionDefault(String param1, List<Integer> list, Map<String, Integer> map) {
        }

        @LoggingAspectConfig(inputCollectionAware = true)
        public void methodInputCollectionAware(String param1, List<Integer> list, Map<String, Integer> map) {
        }

        public void methodInputArray(String [] values){
        }

        @LoggingAspectConfig(inputCollectionAware = true)
        public void methodInputArrayCollectionAware(String [] values){
        }

    }

    @LoggingAspectConfig(inputDetails = false)
    static class TestServiceInput2 {

        public void methodDefaultDetails(String param1, int param2, Double param3) {
        }

        @LoggingAspectConfig(inputIncludeParams = {"param2", "param3"})
        public void methodIncludeParams(String param1, int param2, Double param3) {
        }

    }

    static class TestServiceResult {

        public Object methodReturnNoConfig(){
            throw new UnsupportedOperationException("not supported!");
        }

        @LoggingAspectConfig
        public Object methodReturnDefaultConfig(){
            throw new UnsupportedOperationException("not supported!");
        }

        @LoggingAspectConfig(resultDetails = false)
        public Object methodReturnDetailsSuppressed(){
            throw new UnsupportedOperationException("not supported!");
        }

        @LoggingAspectConfig(resultCollectionAware = false)
        public Object methodReturnPrintWholeCollection(){
            throw new UnsupportedOperationException("not supported!");
        }
    }

}
