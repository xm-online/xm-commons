package com.icthh.xm.commons.logging.configurable;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.icthh.xm.commons.lep.spring.SpringLepManager;
import com.icthh.xm.commons.logging.config.LoggingConfig;
import com.icthh.xm.commons.logging.config.LoggingConfig.LepLogConfiguration;
import com.icthh.xm.commons.logging.config.LoggingConfig.LogConfiguration;
import com.icthh.xm.commons.logging.spring.config.ServiceLoggingAspectConfiguration;
import com.icthh.xm.commons.logging.web.spring.config.RestLoggingAspectConfiguration;
import com.icthh.xm.commons.tenant.PlainTenant;
import com.icthh.xm.commons.tenant.Tenant;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.charset.Charset;
import java.util.Optional;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {LoggingRefreshableConfiguration.class,
        ServiceLoggingAspectConfiguration.class,
        TestService.class,
        TestResource.class,
        RestLoggingAspectConfiguration.class,
        TestConfig.class},
    properties = {"spring.application.name=testApp"})
public class LoggingRefreshableConfigurationUnitTest {

    private static final String UPDATE_KEY = "/config/tenants/test/testApp/logging.yml";
    private static final String CONFIG_1 = "logging-1.yml";
    private static final String CONFIG_2 = "logging-2.yml";
    private static final String TEST_LOG_LEVEL_CONFIG = "test-log-level.yml";
    private static final String TEST_INCLUDE_EXCLUDE_CONFIG = "test-log-input-output.yml";

    @Autowired
    private SpringLepManager lepManager;

    @Autowired
    private TestService testServiceForLogging;

    @Autowired
    private TestResource testResourceForLogging;

    @Autowired
    private LoggingRefreshableConfiguration loggingRefreshableConfiguration;

    @MockBean
    private TenantContextHolder tenantContextHolder;

    @Mock
    private TenantContext tenantContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestAppender.clearEvents();
        when(tenantContextHolder.getContext()).thenReturn(new TenantContext() {
            @Override
            public boolean isInitialized() {
                return true;
            }

            @Override
            public Optional<Tenant> getTenant() {
                return of(new PlainTenant(TenantKey.valueOf("test")));
            }
        });
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("test")));

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContext);
        });
    }


    @Test
    public void testGreetingShouldReturnDefaultMessageTest() throws Exception {
        testResourceForLogging.testMethodFirst("aaaaaaa", "sssssss");
        testServiceForLogging.testMethodSecond("firstArgValue", "secondArtValue");
        testServiceForLogging.testMethodThird("sssssssssss", "sssssss");
    }

    @Test
    public void testUpdateConfiguration() {
        loggingRefreshableConfiguration.onRefresh(UPDATE_KEY, readConfig(CONFIG_1));

        LogConfiguration serviceLoggingConfig =
            loggingRefreshableConfiguration.getServiceLoggingConfig(null, "TestService", "findAll");

        LogConfiguration apiLoggingConfig =
            loggingRefreshableConfiguration.getApiLoggingConfig(null, "TestResource", "testMethodFirst");

        LepLogConfiguration lepLoggingConfig =
            loggingRefreshableConfiguration.getLepLoggingConfig("lep://TEST/function/Function$$RUN_LEP$$tenant.groovy");

        testResourceForLogging.testMethodFirst("firstArgValue", "secondArtValue");

        assertNotNull(apiLoggingConfig);
        assertNotNull(serviceLoggingConfig);
        assertNotNull(lepLoggingConfig);
    }

    @Test
    public void testChangeLogLevel() {
        loggingRefreshableConfiguration.onRefresh(UPDATE_KEY, readConfig(TEST_LOG_LEVEL_CONFIG));

        testServiceForLogging.testMethodFirst("firstArgValue", "secondArtValue");
        Optional<ILoggingEvent> logStart = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getMessage().equals("srv:start: {}, input: {}"))
            .findFirst();
        assertTrue(logStart.isPresent());
        assertEquals(logStart.get().getLevel(), Level.WARN);

        Optional<ILoggingEvent> logStop = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getMessage().equals("srv:stop:  {}, result: {}, time = {} ms"))
            .findFirst();
        assertTrue(logStop.isPresent());
        assertEquals(logStop.get().getLevel(), Level.WARN);
    }

    @Test
    public void testInclude() {
        loggingRefreshableConfiguration.onRefresh(UPDATE_KEY, readConfig(TEST_INCLUDE_EXCLUDE_CONFIG));

        testServiceForLogging.testMethodFirst("firstArgValue", "secondArtValue");
        Optional<ILoggingEvent> logStartWithInclude = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getFormattedMessage()
                .equals("srv:start: TestService:testMethodFirst, input: firstArg=firstArgValue"))
            .findFirst();
        assertTrue(logStartWithInclude.isPresent());

        Optional<ILoggingEvent> logStopHiddenResult = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getMessage().equals("srv:stop:  {}, result: {}, time = {} ms"))
            .findFirst();
        assertTrue(logStopHiddenResult.isPresent());

    }

    @Test
    public void hiddenResult() {
        loggingRefreshableConfiguration.onRefresh(UPDATE_KEY, readConfig(TEST_INCLUDE_EXCLUDE_CONFIG));

        testServiceForLogging.testMethodFirst("firstArgValue", "secondArtValue");

        Optional<ILoggingEvent> logStopHiddenResult = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getMessage().equals("srv:stop:  {}, result: {}, time = {} ms"))
            .findFirst();
        assertTrue(logStopHiddenResult.isPresent());
        assertTrue(logStopHiddenResult.get().getFormattedMessage()
            .contains("srv:stop:  TestService:testMethodFirst, result: result, time ="));

        TestAppender.clearEvents();

        testServiceForLogging.testMethodSecond("firstArgValue", "secondArtValue");

        logStopHiddenResult = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getMessage().equals("srv:stop:  {}, result: {}, time = {} ms"))
            .findFirst();
        assertTrue(logStopHiddenResult.isPresent());
        assertTrue(logStopHiddenResult.get().getFormattedMessage()
            .contains("srv:stop:  TestService:testMethodSecond, result: result, time ="));

    }

    @Test
    public void testGetConfigForClass() {
        loggingRefreshableConfiguration.onRefresh(UPDATE_KEY, readConfig(CONFIG_2));

        LoggingConfig.LogConfiguration serviceLoggingConfig =
            loggingRefreshableConfiguration.getServiceLoggingConfig(null, "TestService", "findAll");

        assertNotNull(serviceLoggingConfig);
    }

    @SneakyThrows
    private String readConfig(String name) {
        return IOUtils.toString(this.getClass().getResourceAsStream("/config/" + name), Charset.defaultCharset());
    }
}

