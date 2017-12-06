package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.UrlLepResourceKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The {@link TargetProceedingLep} class.
 */
@Slf4j
public class TargetProceedingLep extends BaseProceedingLep {

    private final UrlLepResourceKey compositeResourceKey;

    public TargetProceedingLep(LepMethod lepMethod, UrlLepResourceKey compositeResourceKey) {
        super(lepMethod);
        this.compositeResourceKey = compositeResourceKey;
    }

    @Override
    public Object proceed() throws Exception {
        Class<?>[] parameterTypes = getMethodSignature().getParameterTypes();
        if (!ArrayUtils.isEmpty(parameterTypes)) {
            throw new IllegalStateException("Call proceed without parameters on method '"
                                                + getMethodSignature().getName()
                                                + "' with arguments for LEP resource key:"
                                                + compositeResourceKey);
        }

        return invoke();
    }

    @Override
    public Object proceed(Object[] args) throws Exception {
        Class<?>[] parameterTypes = getMethodSignature().getParameterTypes();
        if (ArrayUtils.isEmpty(parameterTypes)) {
            throw new IllegalStateException("Call proceed with parameters on method '"
                                                + getMethodSignature().getName()
                                                + "' without arguments for LEP resource key: "
                                                + compositeResourceKey);
        }

        return invoke(args);
    }

    private Object invoke(Object... args) throws Exception {
        Method method = getMethodSignature().getMethod();
        try {
            return method.invoke(getTarget(), args);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Error while processing target method: " + method, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause == null) {
                throw new IllegalStateException("Invocation exception cause is null, "
                                                    + "while processing target method for LEP resource key: "
                                                    + compositeResourceKey);
            }

            if (cause instanceof Error) {
                throw Error.class.cast(cause);
            } else if (cause instanceof Exception) {
                throw Exception.class.cast(cause);
            } else {
                log.warn("Error execute LEP target method", e);
                throw new IllegalStateException("Error processing target method for LEP resource key: "
                                                    + compositeResourceKey + ". "
                                                    + cause.getMessage(), cause);
            }
        }
    }

}
