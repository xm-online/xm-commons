package com.icthh.xm.commons.logging.aop;

import ch.qos.logback.classic.Level;
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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @MockitoBean
    private LoggingConfigService loggingConfigService;

    @MockitoBean
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
        List<ILoggingEvent> logs = captureLogs(() -> service.testMethod("input"));

        assertTrue(logs.stream()
            .anyMatch(event ->
                event.getLevel() == Level.DEBUG && event.getFormattedMessage().contains("srv:start")));
    }

    @Test
    public void shouldUseDefaultLogLevelWhenAnnotationMissing() {
        List<ILoggingEvent> logs = captureLogs(() -> service.testDefault("input"));

        assertTrue(logs.stream()
            .anyMatch(event -> event.getLevel() == Level.INFO));
    }

    private List<ILoggingEvent> captureLogs(Runnable serviceCall) {
        Logger logger = (Logger) LoggerFactory.getLogger(ServiceLoggingAspect.class);
        logger.setLevel(Level.DEBUG);

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        serviceCall.run();

        return listAppender.list;
    }
}
