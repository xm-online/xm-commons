package com.icthh.xm.commons.metric.service;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessMetricServiceUnitTest {

    private static final String TENANT_KEY = "xm";

    private BusinessMetricsService businessMetricsService;
    private MeterRegistry meterRegistry;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @BeforeEach
    void init() {
        meterRegistry = new SimpleMeterRegistry();
        when(tenantContextHolder.getTenantKey()).thenReturn(TENANT_KEY);
        businessMetricsService = new BusinessMetricsService(meterRegistry, tenantContextHolder);
    }

    @Test
    void incrementCounter() {
        businessMetricsService.incrementCounter("orders.created", Map.of("channel", "web"));

        Counter counter = meterRegistry.find("business.metrics.xm.orders.created")
            .tag("channel", "web")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void incrementCounterByAmount() {
        businessMetricsService.incrementCounter("orders.created", Map.of("channel", "api"), 5.0);

        Counter counter = meterRegistry.find("business.metrics.xm.orders.created")
            .tag("channel", "api")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(5.0);
    }

    @Test
    void incrementCounterMultipleTimes() {
        Map<String, String> tags = Map.of("channel", "web");

        businessMetricsService.incrementCounter("orders.created", tags);
        businessMetricsService.incrementCounter("orders.created", tags);
        businessMetricsService.incrementCounter("orders.created", tags);

        Counter counter = meterRegistry.find("business.metrics.xm.orders.created")
            .tag("channel", "web")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(3.0);
    }

    @Test
    void incrementCounterWithEmptyTags() {
        businessMetricsService.incrementCounter("orders.created", Map.of());

        Counter counter = meterRegistry.find("business.metrics.xm.orders.created").counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void incrementCounterWithNullTags() {
        businessMetricsService.incrementCounter("orders.created", null);

        Counter counter = meterRegistry.find("business.metrics.xm.orders.created").counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordTimer() {
        businessMetricsService.recordTimer(
            "payment.processing",
            Map.of("provider", "stripe"),
            500,
            TimeUnit.MILLISECONDS
        );

        Timer timer = meterRegistry.find("business.metrics.xm.payment.processing")
            .tag("provider", "stripe")
            .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(500.0);
    }

    @Test
    void recordTimerWithSupplier() {
        String result = businessMetricsService.recordTimer(
            "payment.processing",
            Map.of("provider", "stripe"),
            () -> "completed"
        );

        assertThat(result).isEqualTo("completed");

        Timer timer = meterRegistry.find("business.metrics.xm.payment.processing")
            .tag("provider", "stripe")
            .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void recordDistribution() {
        businessMetricsService.recordDistribution(
            "order.amount",
            Map.of("currency", "USD"),
            99.99
        );

        var summary = meterRegistry.find("business.metrics.xm.order.amount")
            .tag("currency", "USD")
            .summary();

        assertThat(summary).isNotNull();
        assertThat(summary.count()).isEqualTo(1);
        assertThat(summary.totalAmount()).isEqualTo(99.99);
    }

    @Test
    void incrementCounterSeparateMetricsByTags() {
        businessMetricsService.incrementCounter("orders.created", Map.of("channel", "web"));
        businessMetricsService.incrementCounter("orders.created", Map.of("channel", "api"));

        Counter webCounter = meterRegistry.find("business.metrics.xm.orders.created")
            .tag("channel", "web")
            .counter();

        Counter apiCounter = meterRegistry.find("business.metrics.xm.orders.created")
            .tag("channel", "api")
            .counter();

        assertThat(webCounter).isNotNull();
        assertThat(webCounter.count()).isEqualTo(1.0);
        assertThat(apiCounter).isNotNull();
        assertThat(apiCounter.count()).isEqualTo(1.0);
    }

    @Test
    void incrementCounterSeparateMetricsByName() {
        Map<String, String> tags = Map.of("channel", "web");

        businessMetricsService.incrementCounter("orders.created", tags);
        businessMetricsService.incrementCounter("orders.cancelled", tags);

        Counter created = meterRegistry.find("business.metrics.xm.orders.created")
            .tag("channel", "web")
            .counter();

        Counter cancelled = meterRegistry.find("business.metrics.xm.orders.cancelled")
            .tag("channel", "web")
            .counter();

        assertThat(created).isNotNull();
        assertThat(created.count()).isEqualTo(1.0);
        assertThat(cancelled).isNotNull();
        assertThat(cancelled.count()).isEqualTo(1.0);
    }

    @Test
    void incrementCounterIncludeMultipleTags() {
        businessMetricsService.incrementCounter(
            "orders.created",
            Map.of("channel", "web", "region", "eu", "priority", "high")
        );

        Counter counter = meterRegistry.find("business.metrics.xm.orders.created")
            .tag("channel", "web")
            .tag("region", "eu")
            .tag("priority", "high")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
