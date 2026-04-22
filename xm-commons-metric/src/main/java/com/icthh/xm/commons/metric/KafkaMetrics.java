package com.icthh.xm.commons.metric;

import static java.util.Objects.nonNull;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaAdmin;

public class KafkaMetrics implements MeterBinder {

    private static final String PROP_METRIC_KAFKA_CONNECTION_SUCCESS = "kafka.connection.success";

    private final KafkaAdmin kafkaAdmin;
    private final Integer connectionTimeoutTopic;
    private final List<String> metricTopics;

    private final Logger log = LoggerFactory.getLogger(KafkaMetrics.class);

    public KafkaMetrics(KafkaAdmin kafkaAdmin, Integer connectionTimeoutTopic, List<String> metricTopics) {
        this.kafkaAdmin = kafkaAdmin;
        this.connectionTimeoutTopic = connectionTimeoutTopic;
        this.metricTopics = metricTopics;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder(PROP_METRIC_KAFKA_CONNECTION_SUCCESS, () -> invokeLong(this::connectionToKafkaTopicsIsSuccess))
            .description("Whether connection to Kafka topics is successful")
            .register(registry);
    }

    public Boolean connectionToKafkaTopicsIsSuccess() {
        if (nonNull(metricTopics) && nonNull(connectionTimeoutTopic)) {
            StopWatch executionTime = StopWatch.createStarted();
            DescribeTopicsOptions describeTopicsOptions = new DescribeTopicsOptions().timeoutMs(
                connectionTimeoutTopic);
            try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
                try {
                    DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(
                        metricTopics,
                        describeTopicsOptions);
                    Map<String, TopicDescription> topicDescriptionMap = describeTopicsResult.allTopicNames().get();
                    boolean monitoringResult = nonNull(topicDescriptionMap);
                    log.info("Connection to Kafka topics is {}, time: {}", monitoringResult, executionTime.getTime());
                    return monitoringResult;
                } catch (Exception e) {
                    log.warn("Exception when try connect to kafka topics: {}, exception: {}, time: {}", metricTopics,
                        e.getMessage(), executionTime.getTime());
                    return false;
                }
            }
        }
        log.warn("metricTopics or connectionTimeoutTopic not found: {}, {}", metricTopics, connectionTimeoutTopic);
        return null;
    }

    private Number invokeLong(Supplier<Boolean> metricsSupplier) {
        return Boolean.TRUE.equals(metricsSupplier.get()) ? 1L : 0L;
    }
}
