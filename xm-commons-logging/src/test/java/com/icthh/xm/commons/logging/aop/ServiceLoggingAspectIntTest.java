package com.icthh.xm.commons.logging.aop;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.spring.config.ServiceLoggingAspectConfiguration;
import com.icthh.xm.commons.logging.util.BasePackageDetector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
    AspectLoggingTestConfig.class,
    ServiceLoggingAspectConfiguration.class
})
@Import({TestServiceWithLogLevel.class})
public class ServiceLoggingAspectIntTest {

    @MockBean
    private LoggingConfigService loggingConfigService;

    @MockBean
    private BasePackageDetector basePackageDetector;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private TestServiceWithLogLevel service;

    @Before
    public void setUp() {
        when(basePackageDetector.getBasePackage()).thenReturn("com.icthh.xm");
        when(loggingConfigService.getServiceLoggingConfig(any(), any(), any())).thenReturn(null);
    }

    @Test
    public void shouldUseLogLevelFromAnnotation() {
        Logger logger = (Logger) LoggerFactory.getLogger(ServiceLoggingAspect.class);

        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        service.testMethod("input");

        List<ILoggingEvent> logsList = listAppender.list;

        assertTrue(
            logsList.stream()
                .anyMatch(event ->
                    event.getLevel() == ch.qos.logback.classic.Level.DEBUG &&
                        event.getFormattedMessage().contains("srv:start")
                )
        );
    }

    @Test
    public void shouldUseDefaultLogLevelWhenAnnotationMissing() {
        Logger logger = (Logger) LoggerFactory.getLogger(ServiceLoggingAspect.class);

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        service.testDefault("input");

        List<ILoggingEvent> logsList = listAppender.list;

        assertTrue(
            logsList.stream()
                .anyMatch(event -> event.getLevel() == ch.qos.logback.classic.Level.INFO)
        );
    }
}
