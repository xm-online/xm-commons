package com.icthh.xm.commons.metric;

import static com.icthh.xm.commons.metric.TenantClientMetricObserver.TAG_CLIENT_ID;
import static com.icthh.xm.commons.metric.TenantClientMetricObserver.TAG_HTTP_STATUS_CODE;
import static com.icthh.xm.commons.metric.TenantClientMetricObserver.TAG_TENANT_CODE;
import static com.icthh.xm.commons.metric.TenantClientMetricObserver.TENANT_CLIENT_REQUESTS;
import static com.icthh.xm.commons.metric.TenantClientMetricObserver.TENANT_REQUESTS;
import static com.icthh.xm.commons.metric.TenantClientMetricObserver.ANONYMOUS_CLIENT;
import static com.icthh.xm.commons.metric.TenantClientMetricObserver.UNKNOWN_TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantClientMetricObserverUnitTest {

    private SimpleMeterRegistry registry;
    private TenantClientMetricObserver observer;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        observer = new TenantClientMetricObserver(registry);
    }

    @Test
    void shouldNotPropagateMetricFailure() {
        registry.config().meterFilter(new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                if (TENANT_REQUESTS.equals(id.getName())) {
                    throw new IllegalStateException("registry failure");
                }
                return id;
            }
        });

        assertThatNoException().isThrownBy(() -> observer.increment("XM", "web_app", 200));

        assertThat(registry.find(TENANT_REQUESTS).counter()).isNull();
    }

    @Test
    void shouldIncrementTenantAndClientCounters() {
        observer.increment("XM", "web_app", 200);
        observer.increment("XM", "web_app", 200);
        observer.increment("XM", "internal", 200);
        observer.increment("XM", "web_app", 404);

        assertThat(tenantCount("XM", "200")).isEqualTo(3);
        assertThat(tenantCount("XM", "404")).isEqualTo(1);
        assertThat(clientCount("XM", "web_app", "200")).isEqualTo(2);
        assertThat(clientCount("XM", "internal", "200")).isEqualTo(1);
        assertThat(clientCount("XM", "web_app", "404")).isEqualTo(1);
    }

    @Test
    void shouldUseUnknownTenantAndAnonymousClientForBlankValues() {
        observer.increment(null, " ", 401);

        assertThat(tenantCount(UNKNOWN_TENANT, "401")).isEqualTo(1);
        assertThat(clientCount(UNKNOWN_TENANT, ANONYMOUS_CLIENT, "401")).isEqualTo(1);
    }

    @Test
    void shouldExportPrometheusNamesWithSingleTotalSuffix() {
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        new TenantClientMetricObserver(prometheusRegistry).increment("XM", "web_app", 200);

        String scrape = prometheusRegistry.scrape();

        assertThat(scrape)
            .contains("tenant_requests_total{httpStatusCode=\"200\",tenantCode=\"XM\"} 1.0")
            .contains("tenant_client_requests_total{clientId=\"web_app\",httpStatusCode=\"200\",tenantCode=\"XM\"} 1.0")
            .doesNotContain("_total_total");
    }

    private double tenantCount(String tenant, String status) {
        return registry.get(TENANT_REQUESTS)
            .tag(TAG_TENANT_CODE, tenant)
            .tag(TAG_HTTP_STATUS_CODE, status)
            .counter()
            .count();
    }

    private double clientCount(String tenant, String clientId, String status) {
        return registry.get(TENANT_CLIENT_REQUESTS)
            .tag(TAG_TENANT_CODE, tenant)
            .tag(TAG_CLIENT_ID, clientId)
            .tag(TAG_HTTP_STATUS_CODE, status)
            .counter()
            .count();
    }
}
