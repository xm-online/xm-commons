package com.icthh.xm.commons.logging.aop;

import static org.junit.Assert.assertEquals;

import com.icthh.xm.commons.logging.spring.config.ServiceLoggingAspectConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test for AOP based logging.
 *
 * This test can be used as playground for pointcut testing.
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {AspectLoggingTestConfig.class, ServiceLoggingAspectConfiguration.class})
public class AspectLoggingUnitTest {

    @Autowired
    TestLoggingAspect testLoggingAspect;

    @Autowired
    TestServiceForLogging service;

    @Test
    public void testPointcutOnInit() {

        service.onInit("key1", "value1");

        assertEquals("key1", testLoggingAspect.getProcessedKey());

        assertEquals("value1", testLoggingAspect.getProcessedConfig());

    }

    @Test
    public void testPointcutOnRefresh() {

        service.onRefresh("key2", "value2");

        assertEquals("key2", testLoggingAspect.getProcessedKey());

        assertEquals("value2", testLoggingAspect.getProcessedConfig());

    }

}
