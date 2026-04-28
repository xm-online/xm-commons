package com.icthh.xm.commons.metric.service;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.impl.DefaultLepKey;
import com.icthh.xm.commons.metric.config.TestLepTestConfig;
import com.icthh.xm.commons.metric.lep.LepEngineMetricsAspect;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.icthh.xm.commons.metric.service.BusinessMetricsService.METRIC_LEP_EXECUTION_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    TestLepTestConfig.class,
    TenantContextConfiguration.class,
    MetricsPercentileHistogramLep.class,
    XmAuthenticationContextConfiguration.class,
    LepEngineMetricsAspect.class,
})
public class MetricsPercentileHistogramLepIntTest {

    @Autowired
    private LepManagementService lepManager;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private TestLepService lepService;

    @Autowired
    private XmLepScriptConfigServerResourceLoader resourceLoader;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private MetricsPercentileHistogramLep metricsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @AfterEach
    public void tearDown() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }


    private void setTenant(String tenantKey) {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        lepManager.beginThreadContext();
    }

    @Test
    public void testRecordTimerWithPercentileHistogramWithRealLep() {
        setTenant("test");

        addLep("return 'lep result'", "test");

        for (int i = 0; i < 10; i++) {
            lepService.testLep();
        }

        verifyPercentileHistogramMetrics();
    }

    private void verifyPercentileHistogramMetrics() {
        Timer timer = meterRegistry.find(METRIC_LEP_EXECUTION_TIME)
            .tag("tenant", "test")
            .tag("lepKey", new DefaultLepKey("services", "TestLep").toString())
            .tag("engine", "GroovyLepEngine")
            .timer();

        assertNotNull(timer);
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(10);

        verifyPercentiles(timer);
    }

    private void verifyPercentiles(Timer timer) {
        assertThat(timer).isNotNull();

        assertNotNull(timer, "Timer not found");

        HistogramSnapshot snapshot = timer.takeSnapshot();

        assertTrue(snapshot.count() > 0, "No recordings in timer");
        assertTrue(snapshot.percentileValues().length > 0, "No histogram data");

    }

    private void addLep(String body, String tenant) {
        String prefix = "/config/tenants/" + tenant.toUpperCase() + "/testApp/lep/services/";
        String key = prefix + "TestLep.groovy";
        resourceLoader.onRefresh(key, body);
    }
}
