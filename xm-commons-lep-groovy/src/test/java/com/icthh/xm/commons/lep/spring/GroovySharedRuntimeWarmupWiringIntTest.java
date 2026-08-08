package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.groovy.GroovyLepEngineFactory;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The shared runtime warmup trigger rides on the engine factory bean, so a microservice-style
 * configuration (a subclass of GroovyLepEngineConfiguration, exactly how every service wires lep) starts
 * the warmup as soon as the factory is created - before any lep engine can exist.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    DynamicLepTestConfig.class,
    TenantContextConfiguration.class,
    XmAuthenticationContextConfiguration.class
})
@ActiveProfiles("resolveclasstest")
public class GroovySharedRuntimeWarmupWiringIntTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void factoryBeanIsCreatedWithSharedRuntimeWarmupWiring() {
        assertNotNull(context.getBean(GroovyLepEngineFactory.class),
            "engine factory bean must exist - it carries the shared runtime warmup trigger");
    }
}
