package com.icthh.xm.commons.logging.web.aop;

import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.util.BasePackageDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = RestLoggingAspect.class)
@Import({TestController.class, IgnoredClassController.class})
@EnableAspectJAutoProxy
public class RestLoggingAspectIntTest {

    @Autowired
    private TestController testController;

    @Autowired
    private IgnoredClassController ignoredClassController;

    @MockBean
    private LoggingConfigService loggingConfigService;

    @MockBean
    private BasePackageDetector basePackageDetector;

    @BeforeEach
    void setUp() {
        when(basePackageDetector.getBasePackage()).thenReturn("com.icthh.xm");
    }

    @Test
    void shouldLogWhenNoIgnoreAnnotation() {
        testController.normalEndpoint();

        verify(loggingConfigService, atLeastOnce()).getApiLoggingConfig(any(), any(), any());
    }

    @Test
    void shouldNotLogWhenMethodHasIgnoreAnnotation() {
        testController.ignoredMethod();

        verify(loggingConfigService, never()).getApiLoggingConfig(any(), any(), any());
    }

    @Test
    void shouldNotLogWhenClassHasIgnoreAnnotation() {
        ignoredClassController.endpoint();

        verify(loggingConfigService, never()).getApiLoggingConfig(any(), any(), any());
    }
}
