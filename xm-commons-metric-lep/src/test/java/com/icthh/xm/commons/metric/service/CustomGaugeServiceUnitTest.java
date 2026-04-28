package com.icthh.xm.commons.metric.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.metric.configuration.CustomGauge;
import com.icthh.xm.commons.tenant.Tenant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomGaugeServiceUnitTest {

    private static final String TENANT = "XM";
    private static final String METRIC_NAME = "orderCount";

    private CustomGaugeService customGaugeService;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private PrivilegedTenantContext privilegedTenantContext;

    @Mock
    private LepManagementService lepManagementService;

    @Mock
    private CustomGaugeService selfMock;

    @BeforeEach
    void init() {
        customGaugeService = new CustomGaugeService(tenantContextHolder, lepManagementService);
        customGaugeService.setSelf(selfMock);

        when(tenantContextHolder.getPrivilegedContext()).thenReturn(privilegedTenantContext);
        when(privilegedTenantContext.execute(any(Tenant.class), any(Supplier.class)))
            .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());
    }

    @Test
    void getMetricReturnDirectValueWhenUpdatePeriodIsNull() {
        CustomGauge gauge = customGauge(METRIC_NAME, null);
        when(selfMock.metricByName(eq(METRIC_NAME))).thenReturn(42);

        Object result = customGaugeService.getMetric(METRIC_NAME, gauge, TENANT);

        assertThat(result).isEqualTo(42);
        verify(selfMock).metricByName(eq(METRIC_NAME));
    }

    @Test
    void getMetricReturnCachedValueWhenUpdatePeriodIsSet() {
        CustomGauge gauge = customGauge(METRIC_NAME, 30);
        when(selfMock.metricByName(eq(METRIC_NAME))).thenReturn(100);

        customGaugeService.updateMetric(METRIC_NAME, TENANT);

        Object result = customGaugeService.getMetric(METRIC_NAME, gauge, TENANT);

        assertThat(result).isEqualTo(100);
    }

    @Test
    void updateMetricWithValueOnTheSameCalls() {
        CustomGauge gauge = customGauge(METRIC_NAME, 30);

        when(selfMock.metricByName(eq(METRIC_NAME))).thenReturn(10);
        customGaugeService.updateMetric(METRIC_NAME, TENANT);
        assertThat(customGaugeService.getMetric(METRIC_NAME, gauge, TENANT)).isEqualTo(10);

        when(selfMock.metricByName(eq(METRIC_NAME))).thenReturn(20);
        customGaugeService.updateMetric(METRIC_NAME, TENANT);
        assertThat(customGaugeService.getMetric(METRIC_NAME, gauge, TENANT)).isEqualTo(20);
    }

    @Test
    void getMetricRemoveMetricFromCacheWhenValueIsNull() {
        when(selfMock.metricByName(eq(METRIC_NAME))).thenReturn(50);
        customGaugeService.updateMetric(METRIC_NAME, TENANT);

        CustomGauge gauge = customGauge(METRIC_NAME, 30);
        assertThat(customGaugeService.getMetric(METRIC_NAME, gauge, TENANT)).isEqualTo(50);

        when(selfMock.metricByName(eq(METRIC_NAME))).thenReturn(null);
        customGaugeService.updateMetric(METRIC_NAME, TENANT);

        assertThat(customGaugeService.getMetric(METRIC_NAME, gauge, TENANT)).isNull();
    }

    @Test
    void getMetricCacheBetweenTenants() {
        String otherTenant = "DEMO";
        CustomGauge gauge = customGauge(METRIC_NAME, 30);

        when(selfMock.metricByName(eq(METRIC_NAME))).thenReturn(10);
        customGaugeService.updateMetric(METRIC_NAME, TENANT);

        when(selfMock.metricByName(eq(METRIC_NAME))).thenReturn(20);
        customGaugeService.updateMetric(METRIC_NAME, otherTenant);

        assertThat(customGaugeService.getMetric(METRIC_NAME, gauge, TENANT)).isEqualTo(10);
        assertThat(customGaugeService.getMetric(METRIC_NAME, gauge, otherTenant)).isEqualTo(20);
    }

    @Test
    void getMetricCacheBetweenOtherMetrics() {
        String otherMetric = "userCount";
        CustomGauge gauge = customGauge(METRIC_NAME, 30);
        CustomGauge gaugeOther = customGauge(otherMetric, 30);

        when(selfMock.metricByName(eq(METRIC_NAME))).thenReturn(100);
        customGaugeService.updateMetric(METRIC_NAME, TENANT);

        when(selfMock.metricByName(eq(otherMetric))).thenReturn(200);
        customGaugeService.updateMetric(otherMetric, TENANT);

        assertThat(customGaugeService.getMetric(METRIC_NAME, gauge, TENANT)).isEqualTo(100);
        assertThat(customGaugeService.getMetric(otherMetric, gaugeOther, TENANT)).isEqualTo(200);
    }

    private CustomGauge customGauge(String name, Integer updatePeriodSeconds) {
        CustomGauge gauge = new CustomGauge();
        gauge.setName(name);
        gauge.setUpdatePeriodSeconds(updatePeriodSeconds);
        return gauge;
    }
}
