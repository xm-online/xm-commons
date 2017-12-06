package com.icthh.xm.commons.lep.spring;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The {@link LepServiceFactoryBean} is a factory for interface based LEP services.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class LepServiceFactoryBean implements FactoryBean<Object>, InitializingBean, ApplicationContextAware {

    // **********************************
    // * WARNING! Nothing in this class should be @Autowired. It causes NPEs because of some lifecycle race condition.
    // **********************************

    /**
     * LEP service interface class.
     */
    private Class<?> type;

    /**
     * LEP service interface Spring bean annotation metadata.
     */
    private AnnotationMetadata annotationMetadata;

    /**
     * Spring application context.
     */
    private ApplicationContext applicationContext;

    /**
     * LEP service handler.
     */
    private LepServiceHandler lepServiceHandler;

    /**
     * Object.class method names.
     */
    private static final Set<String> OBJECT_METHODS = new HashSet<>();

    static {
        OBJECT_METHODS.addAll(Arrays.asList("toString", "getClass", "hashCode", "equals", "clone", "notify",
                                            "notifyAll", "wait", "finalize"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        this.lepServiceHandler = applicationContext.getBean(LepServiceHandler.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private static boolean isToString(Method method) {
        return "toString".equals(method.getName())
            && Objects.equals(method.getReturnType(), String.class)
            && method.getParameterCount() == 0;
    }

    private static boolean isObjectMethod(Method method) {
        return OBJECT_METHODS.contains(method.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getObject() throws Exception {
        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if (isToString(method)) {
                return "LepService proxy for: " + getObjectType().getCanonicalName();
            } else if (isObjectMethod(method)) {
                // other objects methods
                return method.invoke(proxy, args);
            }

            // execute LEP method
            return lepServiceHandler.onMethodInvoke(type, null, method, args);
        };

        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, invocationHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getObjectType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSingleton() {
        return true;
    }

}
