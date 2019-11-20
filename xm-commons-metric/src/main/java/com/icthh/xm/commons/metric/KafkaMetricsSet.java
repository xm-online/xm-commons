package com.icthh.xm.commons.metric;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.apache.commons.lang3.time.StopWatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;

public class KafkaMetricsSet implements MetricSet {

    private KafkaAdmin kafkaAdmin;
    private Integer connectionTimeoutTopic;
    private List<String> metricTopics;

    private final Logger log = LoggerFactory.getLogger(KafkaMetricsSet.class);

    public KafkaMetricsSet(KafkaAdmin kafkaAdmin, Integer connectionTimeoutTopic, List<String> metricTopics) {
        this.kafkaAdmin = kafkaAdmin;
        this.connectionTimeoutTopic = connectionTimeoutTopic;
        this.metricTopics = metricTopics;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        Map<String, Metric> metrics = new HashMap<>();
        metrics.put("connection.success", (Gauge) this::connectionToKafkaTopicsIsSuccess);
        return metrics;
    }

    public Boolean connectionToKafkaTopicsIsSuccess() {
        if (nonNull(metricTopics) && nonNull(connectionTimeoutTopic)) {
            StopWatch executionTime = StopWatch.createStarted();
            DescribeTopicsOptions describeTopicsOptions = new DescribeTopicsOptions().timeoutMs(
                connectionTimeoutTopic);
            try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfig())) {
                try {
                    DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(
                        metricTopics,
                        describeTopicsOptions);
                    Map<String, TopicDescription> topicDescriptionMap = describeTopicsResult.all().get();
                    boolean monitoringResult = nonNull(topicDescriptionMap);
                    log.info("Connection to Kafka topics is {}, time: {}", monitoringResult, executionTime.getTime());
                    return monitoringResult;
                } catch (Exception e) {
                    log.warn("Exception when try connect to kafka topics: {}, exception: {}, time: {}", metricTopics, e.getMessage(), executionTime.getTime());
                    return false;
                }
            }
        }
        log.warn("metricTopics or connectionTimeoutTopic not found: {}, {}", metricTopics, connectionTimeoutTopic);
        return null;
    }

}
