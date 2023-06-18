package com.icthh.xm.commons.lep;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.MethodSignature;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * The {@link TargetProceedingLepUnitTest} class.
 */
public class TargetProceedingLepUnitTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Mock
    private Runnable voidMethodNoArgsMarker;

    @Mock
    private Executor voidMethodWithArgMarker;

    private final Method voidMethodNoArgs = ReflectionUtils.findMethod(getClass(), "voidMethodNoArgs");
    private final Method voidMethodWithArg = ReflectionUtils.findMethod(getClass(), "voidMethodWithArg", Runnable.class);
    private final Method privateVoidMethodNoArgs = ReflectionUtils.findMethod(getClass(), "privateVoidMethodNoArgs");
    private final Method voidMethodNoArgsException = ReflectionUtils.findMethod(getClass(), "voidMethodNoArgsException");
    private final Method voidMethodNoArgsError = ReflectionUtils.findMethod(getClass(), "voidMethodNoArgsError");
    private final Method voidMethodNoArgsCustomThrowable = ReflectionUtils.findMethod(getClass(), "voidMethodNoArgsCustomThrowable");

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void voidMethodNoArgsSuccessCall() throws Exception {
        MethodSignature signature = Mockito.mock(MethodSignature.class);
        when(signature.getParameterTypes()).thenReturn(new Class<?>[0]);
        when(signature.getMethod()).thenReturn(voidMethodNoArgs);

        LepMethod method = Mockito.mock(LepMethod.class);
        when(method.getMethodSignature()).thenReturn(signature);
        when(method.getTarget()).thenReturn(this);

        TargetProceedingLep proceedingLep = new TargetProceedingLep(method, null);

        proceedingLep.proceed();

        verify(voidMethodNoArgsMarker).run();
    }

    @Test
    public void whenVoidMethodNoArgsButWithParamsInSignatureThenThrowsException() throws Exception {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage(Matchers.startsWith("Call proceed without parameters on method"));

        MethodSignature signature = Mockito.mock(MethodSignature.class);
        when(signature.getParameterTypes()).thenReturn(new Class<?>[] {String.class});

        LepMethod method = Mockito.mock(LepMethod.class);
        when(method.getMethodSignature()).thenReturn(signature);

        TargetProceedingLep proceedingLep = new TargetProceedingLep(method, null);
        proceedingLep.proceed();
    }

    @Test
    public void whenVoidMethodWithArgsButNoParamsInSignatureThenThrowsException() throws Exception {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage(Matchers.startsWith("Call proceed with parameters on method"));

        MethodSignature signature = Mockito.mock(MethodSignature.class);
        when(signature.getParameterTypes()).thenReturn(new Class<?>[0]);

        LepMethod method = Mockito.mock(LepMethod.class);
        when(method.getMethodSignature()).thenReturn(signature);

        TargetProceedingLep proceedingLep = new TargetProceedingLep(method, null);
        proceedingLep.proceed(new Object[] {"a", 1});
    }

    @Test
    public void voidMethodWithArgSuccessCall() throws Exception {
        MethodSignature signature = Mockito.mock(MethodSignature.class);
        when(signature.getParameterTypes()).thenReturn(new Class<?>[] {
            Runnable.class
        });
        when(signature.getMethod()).thenReturn(voidMethodWithArg);

        LepMethod method = Mockito.mock(LepMethod.class);
        when(method.getMethodSignature()).thenReturn(signature);
        when(method.getTarget()).thenReturn(this);

        TargetProceedingLep proceedingLep = new TargetProceedingLep(method, null);

        Runnable command = mock(Runnable.class);
        proceedingLep.proceed(new Object[] {command});

        verify(voidMethodWithArgMarker).execute(eq(command));
    }

    @Test
    public void privateMethodThrowException() throws Exception {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage(Matchers.startsWith("Error while processing target method"));

        MethodSignature signature = Mockito.mock(MethodSignature.class);
        when(signature.getParameterTypes()).thenReturn(new Class<?>[0]);
        when(signature.getMethod()).thenReturn(privateVoidMethodNoArgs);

        LepMethod method = Mockito.mock(LepMethod.class);
        when(method.getMethodSignature()).thenReturn(signature);
        when(method.getTarget()).thenReturn(this);

        TargetProceedingLep proceedingLep = new TargetProceedingLep(method, null);

        proceedingLep.proceed();
    }

    @Test
    public void voidMethodNoArgsBodyThrowsException() throws Exception {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Some error");

        MethodSignature signature = Mockito.mock(MethodSignature.class);
        when(signature.getParameterTypes()).thenReturn(new Class<?>[0]);
        when(signature.getMethod()).thenReturn(voidMethodNoArgsException);

        LepMethod method = Mockito.mock(LepMethod.class);
        when(method.getMethodSignature()).thenReturn(signature);
        when(method.getTarget()).thenReturn(this);

        TargetProceedingLep proceedingLep = new TargetProceedingLep(method, null);
        proceedingLep.proceed();
    }

    @Test
    public void voidMethodNoArgsBodyThrowsError() throws Exception {
        expectedEx.expect(InternalError.class);
        expectedEx.expectMessage("JVM error");

        MethodSignature signature = Mockito.mock(MethodSignature.class);
        when(signature.getParameterTypes()).thenReturn(new Class<?>[0]);
        when(signature.getMethod()).thenReturn(voidMethodNoArgsError);

        LepMethod method = Mockito.mock(LepMethod.class);
        when(method.getMethodSignature()).thenReturn(signature);
        when(method.getTarget()).thenReturn(this);

        TargetProceedingLep proceedingLep = new TargetProceedingLep(method, null);
        proceedingLep.proceed();
    }

    @Test
    public void voidMethodNoArgsBodyThrowsCustomThrowable() throws Exception {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage(Matchers.startsWith("Error processing target method for LEP resource key"));

        MethodSignature signature = Mockito.mock(MethodSignature.class);
        when(signature.getParameterTypes()).thenReturn(new Class<?>[0]);
        when(signature.getMethod()).thenReturn(voidMethodNoArgsCustomThrowable);

        LepMethod method = Mockito.mock(LepMethod.class);
        when(method.getMethodSignature()).thenReturn(signature);
        when(method.getTarget()).thenReturn(this);

        TargetProceedingLep proceedingLep = new TargetProceedingLep(method, null);
        proceedingLep.proceed();
    }

    public void voidMethodNoArgs() {
        voidMethodNoArgsMarker.run();
    }

    public void voidMethodWithArg(Runnable command) {
        voidMethodWithArgMarker.execute(command);
    }

    private void privateVoidMethodNoArgs() {
    }

    public void voidMethodNoArgsException() {
        throw new IllegalArgumentException("Some error");
    }

    public void voidMethodNoArgsError() {
        throw new InternalError("JVM error");
    }

    public void voidMethodNoArgsCustomThrowable() throws TestThrowable {
        throw new TestThrowable("JVM error");
    }

    public static class TestThrowable extends Throwable {
        public TestThrowable(String msg) {
            super(msg);
        }
    }

}
