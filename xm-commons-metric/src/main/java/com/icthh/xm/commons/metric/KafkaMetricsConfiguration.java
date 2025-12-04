package com.icthh.xm.commons.metric;

import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@ConditionalOnProperty(
    name = "application.kafka-metric.enabled",
    havingValue = "true"
)
public class KafkaMetricsConfiguration {

    private final KafkaAdmin kafkaAdmin;

    @Value("${application.kafkaMetric.connectionTimeoutTopic:#{null}}")
    private Integer connectionTimeoutTopic;
    @Value("${application.kafkaMetric.metricTopics:#{null}}")
    private List<String> metricTopics;

    public KafkaMetricsConfiguration(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Bean
    public MeterBinder kafkaMetricsBinder() {
        return new KafkaMetrics(kafkaAdmin, connectionTimeoutTopic, metricTopics);
    }
}
