package com.icthh.xm.commons.scheduler.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class SchedulerMetricsSet implements MeterBinder {

    private Counter successMessagesCounter;
    private Counter failedMessagesCounter;
    private final AtomicReference<Instant> lastSuccess = new AtomicReference<>(Instant.MIN);
    private final AtomicReference<Instant> lastError = new AtomicReference<>(Instant.MIN);

    @Override
    public void bindTo(MeterRegistry registry) {
        successMessagesCounter = Counter.builder("success.messages.count")
            .register(registry);

        failedMessagesCounter = Counter.builder("failed.messages.count")
            .register(registry);

        Gauge.builder("success.last.time", lastSuccess, ref -> ref.get().toEpochMilli())
            .register(registry);

        Gauge.builder("failed.last.time", lastError, ref -> ref.get().toEpochMilli())
            .register(registry);
    }

    public void onSuccess() {
        if (successMessagesCounter != null) {
            successMessagesCounter.increment();
        }
        lastSuccess.set(Instant.now());
    }

    public void onError() {
        if (failedMessagesCounter != null) {
            failedMessagesCounter.increment();
        }
        lastError.set(Instant.now());
    }
}
