package com.icthh.xm.commons.metric.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessMetricsService {

    public static final String METRIC_LEP_EXECUTION_TIME = "lep.execution.time";
    public static final String METRIC_LEP_EXECUTION_COUNT = "lep.execution.count";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_ERROR = "error";

    private final MeterRegistry meterRegistry;

    public void incrementCounter(String name, Map<String, String> tags) {
        meterRegistry.counter(name, MetricsTagsUtil.toTags(tags)).increment();
    }

    public void incrementCounter(String name, Map<String, String> tags, double amount) {
        meterRegistry.counter(name, MetricsTagsUtil.toTags(tags)).increment(amount);
    }

    public <T extends Number> void registerGauge(String name, Map<String, String> tags, Supplier<T> valueSupplier) {
        meterRegistry.gauge(name, MetricsTagsUtil.toTags(tags), valueSupplier, s -> s.get().doubleValue());
    }

    public void recordTimer(String name, Map<String, String> tags, long duration, TimeUnit unit) {
        meterRegistry.timer(name, MetricsTagsUtil.toTags(tags)).record(duration, unit);
    }

    public <T> T recordTimer(String name, Map<String, String> tags, Supplier<T> action) {
        return meterRegistry.timer(name, MetricsTagsUtil.toTags(tags)).record(action);
    }

    public void recordDistribution(String name, Map<String, String> tags, double value) {
        meterRegistry.summary(name, MetricsTagsUtil.toTags(tags)).record(value);
    }
}
