package com.icthh.xm.commons.logging.aop;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Test config.
 */
@Configuration
@EnableAspectJAutoProxy
public class AspectLoggingTestConfig {

    @Bean
    public TestLoggingAspect testLoggingAspect() {
        return new TestLoggingAspect();
    }

    @Bean
    public TestServiceForLogging testServiceForLogging() {
        return new TestServiceForLogging.TestServiceForLoggingImpl();
    }

}
