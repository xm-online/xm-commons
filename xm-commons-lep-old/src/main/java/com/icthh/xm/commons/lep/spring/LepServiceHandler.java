package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.XmLepConstants;
import com.icthh.xm.lep.api.LepInvocationCauseException;
import com.icthh.xm.lep.api.LepKey;
import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.DefaultLepMethod;
import com.icthh.xm.lep.api.commons.DefaultMethodSignature;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;
import java.util.Objects;

/**
 * The {@link LepServiceHandler} used for handle any method invocation in any LEP service.
 */
// WARNING: DO NOT remove @Component and don't replace with Spring Java config,
// because of this execution of applicationContext.getBean(LepManager.class) may produce new bean instance instead of
// using same singleton !!!
@Component
@Slf4j
public class LepServiceHandler implements ApplicationContextAware {

    /**
     * Spring application context.
     */
    private ApplicationContext applicationContext;

    /**
     * Processes a LEP method invocation on a proxy instance and returns
     * the result.  This method will be invoked on an invocation handler
     * when a method is invoked on a proxy instance that it is
     * associated with.
     *
     * @param targetType type of LEP service (interface, concrete class)
     * @param target     target LEP service object, can be {@code null}
     * @param method     called LEP method
     * @param args       called LEP method arguments
     * @return LEP method result object
     * @throws Throwable the exception to throw from the method invocation on the proxy instance.
     *                   The exception's type must be assignable either to any of the exception types declared in the
     *                   {@code throws} clause of the interface method or to the
     *                   unchecked exception types {@code java.lang.RuntimeException}
     *                   or {@code java.lang.Error}.  If a checked exception is
     *                   thrown by this method that is not assignable to any of the
     *                   exception types declared in the {@code throws} clause of
     *                   the interface method, then an
     *                   {@link UndeclaredThrowableException} containing the
     *                   exception that was thrown by this method will be thrown by the
     *                   method invocation on the proxy instance.
     */
    @SuppressWarnings("squid:S00112") //suppress throwable warning
    public Object onMethodInvoke(Class<?> targetType, Object target, Method method, Object[] args) throws Throwable {
        LepService typeLepService = targetType.getAnnotation(LepService.class);
        Objects.requireNonNull(typeLepService, "No " + LepService.class.getSimpleName()
            + " annotation for type " + targetType.getCanonicalName());

        LogicExtensionPoint methodLep = AnnotationUtils.getAnnotation(method, LogicExtensionPoint.class);
        // TODO methodLep can be null (if used interface method without @LogicExtensionPoint) !!!

        // create/get key resolver instance
        LepKeyResolver keyResolver = getKeyResolver(methodLep);

        // create base LEP key instance
        LepKey baseLepKey = getBaseLepKey(typeLepService, methodLep, method);

        // create LEP method descriptor
        LepMethod lepMethod = buildLepMethod(targetType, target, method, args);

        // call LepManager to process LEP
        try {
            return getLepManager().processLep(baseLepKey, XmLepConstants.UNUSED_RESOURCE_VERSION, keyResolver, lepMethod);
        } catch (LepInvocationCauseException e) {
            log.debug("Error process target", e);
            throw e.getCause();
        } catch (Exception e) {
            throw e;
        }
    }

    private LepMethod buildLepMethod(Class<?> targetType, Object target, Method method, Object[] args) {
        DefaultMethodSignature signature = new DefaultMethodSignature();
        signature.setName(method.getName());
        signature.setModifiers(method.getModifiers());
        signature.setDeclaringClass(targetType);
        signature.setParameterTypes(method.getParameterTypes());
        signature.setExceptionTypes(method.getExceptionTypes());
        signature.setReturnType(method.getReturnType());
        signature.setMethod(method);

        Parameter[] parameters = method.getParameters();
        String[] parameterNames;
        if (parameters == null) {
            parameterNames = new String[0];
        } else {
            parameterNames = new String[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Parameter p = parameters[i];
                // p.isNamePresent()
                parameterNames[i] = p.getName();
            }
        }
        signature.setParameterNames(parameterNames);

        return new DefaultLepMethod(target, signature, args);
    }

    private static LepKey getBaseLepKey(LepService typeLepService, LogicExtensionPoint methodLep, Method method) {
        Map<String, Object> lepServiceAttrs = AnnotationUtils.getAnnotationAttributes(typeLepService);
        String globalGroupName = (String) lepServiceAttrs.get("group");

        String groupName;
        String keyName;

        if (methodLep == null) {
            groupName = globalGroupName;

            keyName = method.getName();
        } else {
            Map<String, Object> lepAttrs = AnnotationUtils.getAnnotationAttributes(methodLep);
            String lepGroupName = (String) lepAttrs.get("group");

            groupName = (StringUtils.isEmpty(lepGroupName) || lepGroupName.trim().isEmpty())
                ? globalGroupName : lepGroupName;

            keyName = (String) lepAttrs.get("value");
        }

        if (keyName != null && keyName.contains(XmLepConstants.EXTENSION_KEY_SEPARATOR)) {
            throw new IllegalArgumentException("Key name '" + keyName + "' can't contains segments separator: '"
                                                   + XmLepConstants.EXTENSION_KEY_SEPARATOR + "'");
        }

        String segments = groupName + XmLepConstants.EXTENSION_KEY_SEPARATOR + keyName;

        // create Lep key instance
        return new SeparatorSegmentedLepKey(segments, XmLepConstants.EXTENSION_KEY_SEPARATOR, XmLepConstants.EXTENSION_KEY_GROUP_MODE);
    }

    private LepKeyResolver getKeyResolver(LogicExtensionPoint lep) {
        if (lep == null) {
            return null;
        }

        Class<? extends LepKeyResolver> keyResolverClass = lep.resolver();
        if (LepKeyResolver.class.equals(keyResolverClass)) {
            return null;
        }
        return applicationContext.getBean(keyResolverClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private LepManager getLepManager() {
        return applicationContext.getBean(LepManager.class);
    }

}
