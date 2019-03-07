package com.icthh.xm.commons.scheduler.metric;

import com.codahale.metrics.MetricRegistry;
import com.icthh.xm.commons.scheduler.adapter.Bindings;
import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
@ConditionalOnBean(Bindings.class)
@EnableMetrics(proxyTargetClass = true)
public class KafkaOffsetsMetricConfiguration {

    private final MetricRegistry metricRegistry;
    private final KafkaOffsetsMetric kafkaOffsetsMetric;

    @PostConstruct
    public void init() {
        metricRegistry.register("scheduler", kafkaOffsetsMetric);
    }

}
