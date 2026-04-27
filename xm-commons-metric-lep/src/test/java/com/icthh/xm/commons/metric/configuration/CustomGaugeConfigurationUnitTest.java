package com.icthh.xm.commons.metric.configuration;

import com.icthh.xm.commons.metric.service.CustomGaugeService;
import com.icthh.xm.commons.metric.service.PeriodGaugeMetricsService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CustomGaugeConfigurationUnitTest {

    private static final String APP_NAME = "entity";
    private static final String TENANT = "XM";
    private static final String TENANT_LOWER = "xm";
    private static final String CONFIG_KEY = "/config/tenants/XM/entity/gauge-metrics.yml";
    private static final String DEMO_CONFIG_KEY = "/config/tenants/DEMO/entity/gauge-metrics.yml";
    private static final String ORDER_COUNT_METRIC_NAME = "orderCount";
    private static final String CUSTOM_GAUGE_ORDER_COUNT_NAME = "custom.gauge.metrics.xm.orderCount";

    private CustomGaugeConfiguration customGaugeConfiguration;

    private MeterRegistry meterRegistry;

    @Mock
    private CustomGaugeService customGaugeService;

    @Mock
    private PeriodGaugeMetricsService periodGaugeMetricsService;

    @Captor
    private ArgumentCaptor<List<CustomGauge>> metricsCaptor;

    @BeforeEach
    public void init() {
        meterRegistry = new SimpleMeterRegistry();
        customGaugeConfiguration = new CustomGaugeConfiguration(
            meterRegistry,
            customGaugeService,
            periodGaugeMetricsService,
            APP_NAME
        );
    }

    @Test
    public void onRefreshRegisterGauge() {
        String config = "- name: orderCount\n  updatePeriodSeconds: 30\n";

        customGaugeConfiguration.onRefresh(CONFIG_KEY, config);

        Gauge gauge = meterRegistry.find(CUSTOM_GAUGE_ORDER_COUNT_NAME).gauge();
        assertThat(gauge).isNotNull();
    }

    @Test
    public void onRefreshRegisterMultipleGauges() {
        String config = """
            - name: orderCount
              updatePeriodSeconds: 30
            - name: userCount
              updatePeriodSeconds: 60
            """;

        customGaugeConfiguration.onRefresh(CONFIG_KEY, config);

        assertThat(meterRegistry.find(CUSTOM_GAUGE_ORDER_COUNT_NAME).gauge()).isNotNull();
        assertThat(meterRegistry.find("custom.gauge.metrics.xm.userCount").gauge()).isNotNull();
    }

    @Test
    public void onRefreshSchedulePeriodicGauges() {
        String config = "- name: orderCount\n  updatePeriodSeconds: 30\n";

        customGaugeConfiguration.onRefresh(CONFIG_KEY, config);

        verify(periodGaugeMetricsService).scheduleGauges(metricsCaptor.capture(), eq(TENANT));
        List<CustomGauge> captured = metricsCaptor.getValue();

        assertThat(captured).hasSize(1);
        assertThat(captured.getFirst().getName()).isEqualTo(ORDER_COUNT_METRIC_NAME);
        assertThat(captured.getFirst().getUpdatePeriodSeconds()).isEqualTo(30);
    }

    @Test
    public void onRefreshCleanupOnBlankConfig() {
        String config = "- name: orderCount\n  updatePeriodSeconds: 30\n";

        customGaugeConfiguration.onRefresh(CONFIG_KEY, config);

        assertThat(meterRegistry.find(CUSTOM_GAUGE_ORDER_COUNT_NAME).gauge()).isNotNull();

        customGaugeConfiguration.onRefresh(CONFIG_KEY, "");

        assertThat(meterRegistry.find(CUSTOM_GAUGE_ORDER_COUNT_NAME).gauge()).isNull();
        verify(periodGaugeMetricsService, times(2)).cancelGauges(eq(TENANT_LOWER));
    }

    @Test
    public void onRefreshCleanupOldMetricsOnConfigUpdate() {
        String configV1 = "- name: oldMetric\n  updatePeriodSeconds: 30\n";
        String configV2 = "- name: newMetric\n  updatePeriodSeconds: 60\n";

        customGaugeConfiguration.onRefresh(CONFIG_KEY, configV1);
        assertThat(meterRegistry.find("custom.gauge.metrics.xm.oldMetric").gauge()).isNotNull();

        customGaugeConfiguration.onRefresh(CONFIG_KEY, configV2);
        assertThat(meterRegistry.find("custom.gauge.metrics.xm.oldMetric").gauge()).isNull();
        assertThat(meterRegistry.find("custom.gauge.metrics.xm.newMetric").gauge()).isNotNull();
    }

    @Test
    public void onRefreshNotAffectOtherTenantMetrics() {
        String xmConfig = "- name: orderCount\n  updatePeriodSeconds: 30\n";
        String demoConfig = "- name: userCount\n  updatePeriodSeconds: 60\n";

        customGaugeConfiguration.onRefresh(CONFIG_KEY, xmConfig);
        customGaugeConfiguration.onRefresh(DEMO_CONFIG_KEY, demoConfig);

        customGaugeConfiguration.onRefresh(CONFIG_KEY, "");

        assertThat(meterRegistry.find(CUSTOM_GAUGE_ORDER_COUNT_NAME).gauge()).isNull();
        assertThat(meterRegistry.find("custom.gauge.metrics.demo.userCount").gauge()).isNotNull();
    }

    @Test
    public void onRefreshHandleNonNumericMetricValue() {
        String config = "- name: stringMetric\n  updatePeriodSeconds: 30\n";

        customGaugeConfiguration.onRefresh(CONFIG_KEY, config);

        Gauge gauge = meterRegistry.find("custom.gauge.metrics.xm.stringMetric").gauge();
        assertThat(gauge).isNotNull();
    }

}
