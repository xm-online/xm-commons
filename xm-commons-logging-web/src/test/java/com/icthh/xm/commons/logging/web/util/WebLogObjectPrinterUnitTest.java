package com.icthh.xm.commons.logging.web.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.logging.LoggingAspectConfig;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * The {@link WebLogObjectPrinterUnitTest} class.
 */
public class WebLogObjectPrinterUnitTest {

    private static final String STATUS_200_OK = "status=200 OK";
    private static final String STATUS_OK = "status=OK";

    @Mock
    JoinPoint joinPoint;

    @Mock
    MethodSignature ms;

    @Mock
    ResponseEntity responseEntity;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(joinPoint.getSignature()).thenReturn(ms);
    }

    @Test
    public void testPrintRestResult() {
        assertEquals(STATUS_OK + ", body=null",
            WebLogObjectPrinter.printRestResult(joinPoint, null).toString());
        assertEquals(STATUS_OK + ", body=value1",
            WebLogObjectPrinter.printRestResult(joinPoint, "value1").toString());
        assertEquals(STATUS_OK + ", body=#hidden#",
            WebLogObjectPrinter.printRestResult(joinPoint, "value1", false).toString());
        assertEquals(STATUS_OK + ", body=value1",
            WebLogObjectPrinter.printRestResult(joinPoint, "value1", true).toString());
        assertEquals(STATUS_OK + ", body=[<ArrayList> size = 5]",
            WebLogObjectPrinter.printRestResult(joinPoint, Arrays.asList(1, 2, 3, 4, 5)).toString());
        assertEquals(STATUS_OK + ", body=#hidden#",
            WebLogObjectPrinter.printRestResult(joinPoint, Arrays.asList(1, 2, 3, 4, 5), false).toString());

        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);

        when(responseEntity.getBody()).thenReturn(null);
        assertEquals(STATUS_200_OK + ", body=null",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity).toString());

        when(responseEntity.getBody()).thenReturn("value1");
        assertEquals(STATUS_200_OK + ", body=value1",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity).toString());

        when(responseEntity.getBody()).thenReturn(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(STATUS_200_OK + ", body=[<ArrayList> size = 5]",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity).toString());

        when(responseEntity.getBody()).thenReturn(null);
        assertEquals(STATUS_200_OK + ", body=#hidden#",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity, false).toString());

        when(responseEntity.getBody()).thenReturn("value1");
        assertEquals(STATUS_200_OK + ", body=#hidden#",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity, false).toString());

        when(responseEntity.getBody()).thenReturn(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(STATUS_200_OK + ", body=#hidden#",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity, false).toString());

        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(responseEntity.getBody()).thenReturn("value1");
        assertEquals("status=400 BAD_REQUEST, body=value1",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity).toString());
    }

    @Test
    public void testPrintRestNoConfig() throws NoSuchMethodException {
        Class<?> aClass = TestRest.class;
        Method method = aClass.getMethod("methodReturnNoConfig");

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);

        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);

        when(responseEntity.getBody()).thenReturn(null);
        assertEquals(STATUS_200_OK + ", body=null",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity).toString());

        when(responseEntity.getBody()).thenReturn("value1");
        assertEquals(STATUS_200_OK + ", body=value1",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity).toString());

        when(responseEntity.getBody()).thenReturn(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(STATUS_200_OK + ", body=[<ArrayList> size = 5]",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity).toString());

        when(responseEntity.getBody()).thenReturn(null);
        assertEquals(STATUS_200_OK + ", body=#hidden#",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity, false).toString());

        when(responseEntity.getBody()).thenReturn("value1");
        assertEquals(STATUS_200_OK + ", body=#hidden#",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity, false).toString());

        when(responseEntity.getBody()).thenReturn(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(STATUS_200_OK + ", body=#hidden#",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity, false).toString());
    }

    @Test
    public void testPrintRestDetailsSuppressed() throws NoSuchMethodException {
        Class<?> aClass = TestRest.class;
        Method method = aClass.getMethod("methodReturnDetailsSuppressed");

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);

        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);

        when(responseEntity.getBody()).thenReturn(null);
        assertEquals(STATUS_200_OK + ", body=#hidden#",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity).toString());

        when(responseEntity.getBody()).thenReturn("value1");
        assertEquals(STATUS_200_OK + ", body=#hidden#",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity).toString());

        when(responseEntity.getBody()).thenReturn(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(STATUS_200_OK + ", body=#hidden#",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity).toString());

        when(responseEntity.getBody()).thenReturn(null);
        assertEquals(STATUS_200_OK + ", body=#hidden#",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity, false).toString());

        when(responseEntity.getBody()).thenReturn("value1");
        assertEquals(STATUS_200_OK + ", body=#hidden#",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity, false).toString());

        when(responseEntity.getBody()).thenReturn(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(STATUS_200_OK + ", body=#hidden#",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity, false).toString());
    }

    @Test
    public void testPrintRestPrintWholeCollection() throws NoSuchMethodException {
        Class<?> aClass = TestRest.class;
        Method method = aClass.getMethod("methodReturnDetailsPrintCollection");

        when(ms.getDeclaringType()).thenReturn(aClass);
        when(ms.getMethod()).thenReturn(method);

        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);

        when(responseEntity.getBody()).thenReturn(null);
        assertEquals(STATUS_200_OK + ", body=null",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity).toString());

        when(responseEntity.getBody()).thenReturn("value1");
        assertEquals(STATUS_200_OK + ", body=value1",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity).toString());

        when(responseEntity.getBody()).thenReturn(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(STATUS_200_OK + ", body=[1, 2, 3, 4, 5]",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity).toString());

        when(responseEntity.getBody()).thenReturn(null);
        assertEquals(STATUS_200_OK + ", body=null",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity, false).toString());

        when(responseEntity.getBody()).thenReturn("value1");
        assertEquals(STATUS_200_OK + ", body=value1",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity, false).toString());

        when(responseEntity.getBody()).thenReturn(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(STATUS_200_OK + ", body=[1, 2, 3, 4, 5]",
            WebLogObjectPrinter.printRestResult(joinPoint, responseEntity, false).toString());
    }

    static class TestRest {

        public Object methodReturnNoConfig() {
            throw new UnsupportedOperationException("not supported!");
        }

        @LoggingAspectConfig(resultDetails = false)
        public Object methodReturnDetailsSuppressed() {
            throw new UnsupportedOperationException("not supported!");
        }

        @LoggingAspectConfig(resultCollectionAware = false)
        public Object methodReturnDetailsPrintCollection() {
            throw new UnsupportedOperationException("not supported!");
        }

    }

}
