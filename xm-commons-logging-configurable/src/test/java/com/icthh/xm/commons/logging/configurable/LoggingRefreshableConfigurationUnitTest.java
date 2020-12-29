package com.icthh.xm.commons.logging.configurable;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.icthh.xm.commons.lep.spring.SpringLepManager;
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

    private static final String UPDATE_KEY = "/config/tenants/TEST/testApp/logging.yml";
    private static final String TEST_LOG_UPDATE_CONFIG = "logging.yml";
    private static final String TEST_CLASS_CONFIG = "logging-class.yml";
    private static final String TEST_INCLUDE_EXCLUDE_CONFIG = "logging-include-exclude.yml";
    private static final String TEST_DISABLE_CONFIG = "logging-disable.yml";

    @Autowired
    private SpringLepManager lepManager;

    @Autowired
    private TestService testService;

    @Autowired
    private TestResource testResource;

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
                return of(new PlainTenant(TenantKey.valueOf("TEST")));
            }
        });
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContext);
        });
    }


    @Test
    public void testGreetingShouldReturnDefaultMessageTest() throws Exception {

        testService.testMethodSecond("firstArgValue", "secondArtValue");
        loggingRefreshableConfiguration.onRefresh(UPDATE_KEY, readConfig(TEST_LOG_UPDATE_CONFIG));

    }

    @Test
    public void testUpdateConfiguration() {
        loggingRefreshableConfiguration.onRefresh(UPDATE_KEY, readConfig(TEST_LOG_UPDATE_CONFIG));

        //service
        LogConfiguration serviceLoggingConfig =
            loggingRefreshableConfiguration.getServiceLoggingConfig(null, "TestService", "testMethodFirst");
        assertNotNull(serviceLoggingConfig);

        //api
        LogConfiguration apiLoggingConfig =
            loggingRefreshableConfiguration.getApiLoggingConfig(null, "TestResource", "testMethodFirst");
        assertNotNull(apiLoggingConfig);

        //lep
        LepLogConfiguration lepLoggingConfig =
            loggingRefreshableConfiguration.getLepLoggingConfig("lep://TEST/general/TestLep$$default.groovy");
        assertNotNull(lepLoggingConfig);
    }

    @Test
    public void testChangeLogLevel() {
        loggingRefreshableConfiguration.onRefresh(UPDATE_KEY, readConfig(TEST_LOG_UPDATE_CONFIG));

        // service
        testService.testMethodFirst("firstArgValue", "secondArgValue");
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

        //api
        testResource.testMethodFirst("firstArgValue", "secondArgValue");
        logStart = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getMessage().equals("START {} : {} --> {}, input: {}"))
            .findFirst();
        assertTrue(logStart.isPresent());
        assertEquals(logStart.get().getLevel(), Level.WARN);

        logStop = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getMessage().equals("STOP  {} : {} --> {}, result: {}, time = {} ms"))
            .findFirst();
        assertTrue(logStop.isPresent());
        assertEquals(logStop.get().getLevel(), Level.WARN);

        //lep
        testService.testMethodSecond("firstArgValue", "secondArgValue");
        logStart = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getMessage().equals("lep:start: execute lep at [{}], script: {}"))
            .findFirst();
        assertTrue(logStart.isPresent());
        assertEquals(logStart.get().getLevel(), Level.WARN);

        logStop = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getMessage().equals("lep:stop:  execute lep at [{}], script: {}"))
            .findFirst();
        assertTrue(logStop.isPresent());
        assertEquals(logStop.get().getLevel(), Level.WARN);
    }

    @Test
    public void testIncludeExcludeInput() {
        loggingRefreshableConfiguration.onRefresh(UPDATE_KEY, readConfig(TEST_INCLUDE_EXCLUDE_CONFIG));

        //service include
        testService.testMethodFirst("firstArgValue", "secondArgValue");
        Optional<ILoggingEvent> logStartWithInclude = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getFormattedMessage()
                .equals("srv:start: TestService:testMethodFirst, input: firstArg=firstArgValue"))
            .findFirst();
        assertTrue(logStartWithInclude.isPresent());

        //service exclude
        testService.testMethodSecond("firstArgValue", "secondArgValue");
        Optional<ILoggingEvent> logStartWithExclude = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getFormattedMessage()
                .equals("srv:start: TestService:testMethodSecond, input: secondArg=secondArgValue"))
            .findFirst();
        assertTrue(logStartWithExclude.isPresent());

        //api include
        testResource.testMethodFirst("firstArgValue", "secondArgValue");
        logStartWithInclude = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getFormattedMessage()
                .equals("START GET : /api/first --> TestResource:testMethodFirst, input: firstArg=firstArgValue"))
            .findFirst();
        assertTrue(logStartWithInclude.isPresent());

        //api exclude
        testResource.testMethodSecond("firstArgValue", "secondArgValue");
        logStartWithExclude = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getFormattedMessage()
                .equals("START GET : /api/second --> TestResource:testMethodSecond, input: secondArg=secondArgValue"))
            .findFirst();
        assertTrue(logStartWithExclude.isPresent());
    }

    @Test
    public void testHiddenResult() {
        loggingRefreshableConfiguration.onRefresh(UPDATE_KEY, readConfig(TEST_INCLUDE_EXCLUDE_CONFIG));

        //service
        testService.testMethodFirst("firstArgValue", "secondArgValue");
        Optional<ILoggingEvent> logStopHiddenResult = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getMessage().equals("srv:stop:  {}, result: {}, time = {} ms"))
            .findFirst();
        assertTrue(logStopHiddenResult.isPresent());
        assertTrue(logStopHiddenResult.get().getFormattedMessage()
            .contains("srv:stop:  TestService:testMethodFirst, result: #hidden#, time ="));

        TestAppender.clearEvents();
        testService.testMethodSecond("firstArgValue", "secondArgValue");
        logStopHiddenResult = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getMessage().equals("srv:stop:  {}, result: {}, time = {} ms"))
            .findFirst();
        assertTrue(logStopHiddenResult.isPresent());
        assertTrue(logStopHiddenResult.get().getFormattedMessage()
            .contains("srv:stop:  TestService:testMethodSecond, result: lepResult, time ="));

        //api
        testResource.testMethodFirst("firstArgValue", "secondArgValue");
        logStopHiddenResult = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getMessage().equals("STOP  {} : {} --> {}, result: {}, time = {} ms"))
            .findFirst();
        assertTrue(logStopHiddenResult.isPresent());
        assertTrue(logStopHiddenResult.get().getFormattedMessage()
            .contains("STOP  GET : /api/first --> TestResource:testMethodFirst, result: status=OK, body=#hidden#"));

        TestAppender.clearEvents();
        testResource.testMethodSecond("firstArgValue", "secondArgValue");
        logStopHiddenResult = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getMessage().equals("STOP  {} : {} --> {}, result: {}, time = {} ms"))
            .findFirst();
        assertTrue(logStopHiddenResult.isPresent());
        assertTrue(logStopHiddenResult.get().getFormattedMessage()
            .contains("STOP  GET : /api/second --> TestResource:testMethodSecond, result: status=OK, body=result"));
    }

    @Test
    public void testClassConfig() {
        loggingRefreshableConfiguration.onRefresh(UPDATE_KEY, readConfig(TEST_CLASS_CONFIG));

        //service
        testService.testMethodFirst("firstArgValue", "secondArgValue");
        Optional<ILoggingEvent> logStart = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getMessage().equals("srv:start: {}, input: {}"))
            .findFirst();
        assertTrue(logStart.isPresent());
        assertEquals(logStart.get().getLevel(), Level.ERROR);

        //api
        testResource.testMethodFirst("firstArgValue", "secondArgValue");
        logStart = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getMessage().equals("START {} : {} --> {}, input: {}"))
            .findFirst();
        assertTrue(logStart.isPresent());
        assertEquals(logStart.get().getLevel(), Level.ERROR);

    }

    @Test
    public void testDisableLog() {
        loggingRefreshableConfiguration.onRefresh(UPDATE_KEY, readConfig(TEST_DISABLE_CONFIG));

        //service
        testService.testMethodSecond("firstArgValue", "secondArgValue");
        Optional<ILoggingEvent> logStart = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getMessage().equals("srv:start: {}, input: {}"))
            .findFirst();
        assertTrue(logStart.isEmpty());

        //api
        testResource.testMethodFirst("firstArgValue", "secondArgValue");
        logStart = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getMessage().equals("START {} : {} --> {}, input: {}"))
            .findFirst();
        assertTrue(logStart.isEmpty());

        //lep
        logStart = TestAppender.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getMessage().equals("lep:start: execute lep at [{}], script: {}"))
            .findFirst();
        assertTrue(logStart.isEmpty());
    }

    @SneakyThrows
    private String readConfig(String name) {
        return IOUtils.toString(this.getClass().getResourceAsStream("/config/" + name), Charset.defaultCharset());
    }
}

