package com.icthh.xm.commons.metric.configuration;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.metric.config.TestLepTestConfig;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.InputStream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    TenantContextConfiguration.class,
    XmAuthenticationContextConfiguration.class,
    TestGaugeConfiguration.class,
    TestLepTestConfig.class
})
@TestPropertySource(properties = {
    "spring.application.name=testApp"
})
public class CustomGaugeConfigurationIntTest {

    @Autowired
    private LepManagementService lepManagerService;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private CustomGaugeConfiguration customGaugeConfiguration;

    @Autowired
    private XmLepScriptConfigServerResourceLoader resourceLoader;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    public void init() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
        lepManagerService.beginThreadContext();
    }

    @AfterEach
    public void tearDown() {
        lepManagerService.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void onRefreshUseLepScripts() {
        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/gauges/Gauge$$around.groovy",
            loadFile("lep/gauge/Gauge$$around.groovy"));
        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/gauges/Gauge$$TEST$$around.groovy",
            loadFile("lep/gauge/Gauge$$TEST$$around.groovy"));

        customGaugeConfiguration.onRefresh("/config/tenants/TEST/testApp/gauge-metrics.yml", loadFile("config/gauge-metrics.yml"));

        Gauge testMetricGauge = meterRegistry.find("custom.gauge.metrics.test.TEST").gauge();
        assertThat(testMetricGauge).isNotNull();
        assertThat(testMetricGauge.value()).isEqualTo(5.0);

        Gauge testMetricGauge2 = meterRegistry.find("custom.gauge.metrics.test.TEST2").gauge();
        assertThat(testMetricGauge2).isNotNull();
        assertThat(testMetricGauge2.value()).isEqualTo(2.0);
    }

    @SneakyThrows
    public static String loadFile(String path) {
        try (InputStream cfgInputStream = new ClassPathResource(path).getInputStream()) {
            return IOUtils.toString(cfgInputStream, UTF_8);
        }
    }
}
