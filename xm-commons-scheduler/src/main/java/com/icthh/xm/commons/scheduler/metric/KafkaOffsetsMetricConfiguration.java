package com.icthh.xm.commons.scheduler.metric;

import com.codahale.metrics.MetricRegistry;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
@ConditionalOnProperty("application.scheduler-enabled")
public class KafkaOffsetsMetricConfiguration {

    private final MetricRegistry metricRegistry;
    private final KafkaOffsetsMetric kafkaOffsetsMetric;

    @PostConstruct
    public void init() {
        metricRegistry.register("scheduler", kafkaOffsetsMetric);
    }

}
