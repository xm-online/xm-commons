package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.groovy.annotation.LepServiceTransformation;
import com.icthh.xm.commons.lep.spring.DynamicLepTestConfig;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduces the production start order: the lep engine init is driven by config events and needs ONLY the
 * engine factory bean - it does not wait for the rest of a minutes-long context initialization. Every bean
 * here is lazy, so asking for the factory is exactly what the config listener does in a real service. The
 * shared runtime warmup must already be started at that moment, otherwise the engine warmup's await is a
 * no-op and the first lep execution pays the receiver metaclass initialization inside a request.
 *
 * <p>This test is red when the warmup trigger lives on a standalone bean (nothing depends on it, spring
 * creates it at an arbitrary later point) and green when it rides on the factory.
 */
public class SharedRuntimeWarmupStartOrderIntTest {

    @BeforeEach
    @AfterEach
    public void resetWarmupState() {
        GroovySharedRuntimeWarmup.awaitCompletion();
        GroovySharedRuntimeWarmup.resetForTests();
    }

    /**
     * Same start-order contract for the lep AST transformation state: engine warmup compiles leps as soon
     * as the factory exists, and a compile with uninitialized {@code LepServiceTransformation} statics
     * fails with an NPE - the lep is then silently missing from the warmup (observed in production as
     * "Error during LepServiceTransformation" and a smaller warmed-classes count).
     */
    @Test
    public void lepServiceTransformationIsInitializedAsSoonAsTheEngineFactoryExists() throws Exception {
        resetLepServiceTransformationState();
        try (AnnotationConfigApplicationContext context = lazyContext()) {
            context.getBean(GroovyLepEngineFactory.class);

            assertTrue(isLepServiceTransformationInitialized(),
                "LepServiceTransformation must be initialized no later than the engine factory exists, "
                    + "otherwise lep compiles during warmup fail with NPE and leave leps cold");
        }
    }

    @Test
    public void sharedWarmupIsStartedAsSoonAsTheEngineFactoryExists() throws Exception {
        try (AnnotationConfigApplicationContext context = lazyContext()) {
            // the config listener path: engine init resolves the factory bean and nothing else
            context.getBean(GroovyLepEngineFactory.class);

            assertTrue(isSharedWarmupStarted(),
                "shared runtime warmup must be started no later than the engine factory exists, "
                    + "otherwise engine warmup cannot await it and the first lep execution pays the cost");
        }
    }

    /**
     * Every singleton is lazy: nothing is instantiated at refresh, exactly like the huge real context
     * where most beans are still pending when the config listener starts engine init.
     */
    private AnnotationConfigApplicationContext lazyContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles("resolveclasstest");
        context.register(DynamicLepTestConfig.class, TenantContextConfiguration.class,
            XmAuthenticationContextConfiguration.class);
        context.addBeanFactoryPostProcessor(beanFactory -> {
            for (String name : beanFactory.getBeanDefinitionNames()) {
                beanFactory.getBeanDefinition(name).setLazyInit(true);
            }
        });
        context.refresh();
        return context;
    }

    private boolean isSharedWarmupStarted() throws Exception {
        Field started = GroovySharedRuntimeWarmup.class.getDeclaredField("STARTED");
        started.setAccessible(true);
        return ((AtomicBoolean) started.get(null)).get();
    }

    private void resetLepServiceTransformationState() throws Exception {
        for (String fieldName : List.of("LEP_CONTEXT_FIELDS", "LEP_CONTEXT_TYPE_HIERARCHY", "LEP_CONTEXT_CLASS_HIERARCHY")) {
            Field field = LepServiceTransformation.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, null);
        }
    }

    private boolean isLepServiceTransformationInitialized() throws Exception {
        Field field = LepServiceTransformation.class.getDeclaredField("LEP_CONTEXT_CLASS_HIERARCHY");
        field.setAccessible(true);
        return field.get(null) != null;
    }
}
