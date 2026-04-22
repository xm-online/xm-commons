package com.icthh.xm.commons.metric.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessMetricsService {

    private final MeterRegistry meterRegistry;

    public void incrementCounter(String name, Map<String, String> tags) {
        meterRegistry.counter(name, toTags(tags)).increment();
    }

    public void incrementCounter(String name, Map<String, String> tags, double amount) {
        meterRegistry.counter(name, toTags(tags)).increment(amount);
    }

    public <T extends Number> void registerGauge(String name, Map<String, String> tags, Supplier<T> valueSupplier) {
        meterRegistry.gauge(name, toTags(tags), valueSupplier, s -> s.get().doubleValue());
    }

    public void recordTimer(String name, Map<String, String> tags, long duration, TimeUnit unit) {
        meterRegistry.timer(name, toTags(tags)).record(duration, unit);
    }

    public <T> T recordTimer(String name, Map<String, String> tags, Supplier<T> action) {
        return meterRegistry.timer(name, toTags(tags)).record(action);
    }

    public void recordDistribution(String name, Map<String, String> tags, double value) {
        meterRegistry.summary(name, toTags(tags)).record(value);
    }

    private Tags toTags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Tags.empty();
        }
        return Tags.of(tags.entrySet().stream()
            .map(e -> Tag.of(e.getKey(), e.getValue()))
            .toList());
    }
}
