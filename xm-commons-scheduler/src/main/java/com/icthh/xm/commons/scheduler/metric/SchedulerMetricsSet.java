package com.icthh.xm.commons.scheduler.metric;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class SchedulerMetricsSet implements MetricSet {

    private final AtomicLong countSuccessMessages = new AtomicLong(0L);
    private final AtomicLong countFailedMessages = new AtomicLong(0L);
    private final AtomicReference<Instant> lastSuccess = new AtomicReference<>(Instant.MIN);
    private final AtomicReference<Instant> lastError = new AtomicReference<>(Instant.MIN);

    @Override
    public Map<String, Metric> getMetrics() {
        Map<String, Metric> metrics = new HashMap<>();
        metrics.put("success.messages.count", (Gauge) countSuccessMessages::get);
        metrics.put("success.last.time", (Gauge) () -> lastSuccess.get().toString());
        metrics.put("failed.messages.count", (Gauge) countFailedMessages::get);
        metrics.put("failed.last.time", (Gauge) () -> lastError.get().toString());
        return metrics;
    }

    public void onSuccess() {
        countSuccessMessages.incrementAndGet();
        lastSuccess.set(Instant.now());
    }

    public void onError() {
        countFailedMessages.incrementAndGet();
        lastError.set(Instant.now());
    }
}
