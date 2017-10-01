package com.icthh.xm.commons.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;

/**
 * Test for log object printer.
 * Created by medved on 27.06.17.
 */
public class LogObjectPrinterUnitTest {

    @Mock
    JoinPoint joinPoint;

    @Mock
    Signature signature;

    @Mock
    MethodSignature ms;

    @Mock
    JoinPoint.StaticPart staticPart;

    @Mock
    ResponseEntity responseEntity;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getStaticPart()).thenReturn(staticPart);

        when(staticPart.getSignature()).thenReturn(ms);
        when(ms.getParameterNames()).thenReturn(new String[] {"param1", "param2", "param3"});
        when(joinPoint.getArgs()).thenReturn(new Object[] {"val1", "val2", "val3"});

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
        assertTrue(StringUtils.contains(LogObjectPrinter.printExceptionWithStackInfo(new RuntimeException("Some error")),
                                        "java.lang.RuntimeException: Some error com.icthh.xm.commons.logging."
                                            + "LogObjectPrinterUnitTest.testPrintExceptionWithStacktraceInfo"));
    }

    @Test
    public void testComposeUrl() {

        String[] arr1 = new String[] {"/first", "/second"};
        String[] arr2 = new String[] {"/third", "/forth"};

        assertEquals("", LogObjectPrinter.composeUrl(null));
        assertEquals("", LogObjectPrinter.composeUrl(null, null));

        assertEquals("/first/second", LogObjectPrinter.composeUrl(arr1));
        assertEquals("/third/forth", LogObjectPrinter.composeUrl(arr2));

        assertEquals("/first/second", LogObjectPrinter.composeUrl(arr1, null));
        assertEquals("/third/forth", LogObjectPrinter.composeUrl(null, arr2));

        Integer[] arrInt = new Integer[] {1, 2, 3};

        assertEquals(
            "printerror:java.lang.IllegalArgumentException: Cannot store java.lang.Integer in an array " +
                      "of java.lang.String",
                      LogObjectPrinter.composeUrl(arr1, arrInt));

    }

    @Test
    public void testGetCallMethod() {

        assertEquals("?:?", LogObjectPrinter.getCallMethod(null));

        when(signature.getDeclaringType()).thenReturn(null);
        assertEquals("?:null", LogObjectPrinter.getCallMethod(joinPoint));

        when(signature.getDeclaringType()).thenReturn(String.class);
        when(signature.getName()).thenReturn("toString");
        assertEquals("String:toString", LogObjectPrinter.getCallMethod(joinPoint));

    }

    @Test
    public void testPrintInputParams() {
        assertEquals("joinPoint is null", LogObjectPrinter.printInputParams(null));
        assertEquals("param1=val1,param2=val2,param3=val3", LogObjectPrinter.printInputParams(joinPoint));

        when(ms.getParameterNames()).thenReturn(new String[] {"param1", "password", "param3"});
        assertEquals("param1=val1,password=*****,param3=val3", LogObjectPrinter.printInputParams(joinPoint));

    }

    @Test
    public void testPrintCollectionAware() {
        assertEquals("", LogObjectPrinter.printCollectionAware(null));
        assertEquals("string1", LogObjectPrinter.printCollectionAware("string1"));
        assertEquals("[<ArrayList> size = 5]", LogObjectPrinter.printCollectionAware(Arrays.asList(1, 2, 3, 4, 5)));
    }

    @Test
    public void testPrintRestResult() {
        assertEquals("status=OK, body=null", LogObjectPrinter.printRestResult(null).toString());
        assertEquals("status=OK, body=value1", LogObjectPrinter.printRestResult("value1").toString());
        assertEquals("status=OK, body=[<ArrayList> size = 5]",
                     LogObjectPrinter.printRestResult(Arrays.asList(1, 2, 3, 4, 5)).toString());

        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);

        when(responseEntity.getBody()).thenReturn(null);
        assertEquals("status=200, body=", LogObjectPrinter.printRestResult(responseEntity).toString());

        when(responseEntity.getBody()).thenReturn("value1");
        assertEquals("status=200, body=value1", LogObjectPrinter.printRestResult(responseEntity).toString());

        when(responseEntity.getBody()).thenReturn(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals("status=200, body=[<ArrayList> size = 5]",
                     LogObjectPrinter.printRestResult(responseEntity).toString());

    }

}
