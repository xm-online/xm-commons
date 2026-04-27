package com.icthh.xm.commons.metric.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Supplier;


@Component
@RequiredArgsConstructor
public class MetricsPercentileHistogramLep {

    private final MeterRegistry meterRegistry;


    public <T> T recordTimerWithPercentileHistogram(String name, Map<String, String> tags, Supplier<T> action) {
        return Timer.builder(name)
                .tags(MetricsTagsUtil.toTags(tags))
                .publishPercentileHistogram(true)
                .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                .register(meterRegistry)
                .record(action);
    }
}
