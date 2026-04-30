package com.icthh.xm.commons.metric.lep;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.impl.DefaultLepKey;
import com.icthh.xm.commons.metric.config.TestLepTestConfig;
import com.icthh.xm.commons.metric.service.TestLepService;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.icthh.xm.commons.metric.service.BusinessMetricsService.METRIC_LEP_EXECUTION_TIME;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    TestLepTestConfig.class,
    TenantContextConfiguration.class,
    XmAuthenticationContextConfiguration.class,
    LepEngineMetricsAspect.class,
})
@TestPropertySource(properties = {
    "xm.commons.lep.metrics.enabled=false"
})
public class LepEngineMetricsAspectDisabledIntTest {

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
    public void testMetricsDisabled() {
        setTenant("test");

        addLep("return 'lep result'", "test");

        for (int i = 0; i < 10; i++) {
            String result = lepService.testLep();
            assertThat(result).isEqualTo("lep result");
        }

        verifyNoMetricsRecorded();
    }

    private void verifyNoMetricsRecorded() {
        Timer timer = meterRegistry.find(METRIC_LEP_EXECUTION_TIME)
            .tag("tenant", "test")
            .tag("lepKey", new DefaultLepKey("services", "TestLep").toString())
            .tag("engine", "GroovyLepEngine")
            .timer();

        assertThat(timer).isNull();
    }

    private void addLep(String body, String tenant) {
        String prefix = "/config/tenants/" + tenant.toUpperCase() + "/testApp/lep/services/";
        String key = prefix + "TestLep.groovy";
        resourceLoader.onRefresh(key, body);
    }
}
