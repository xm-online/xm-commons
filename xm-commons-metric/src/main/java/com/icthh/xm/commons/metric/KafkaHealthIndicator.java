package com.icthh.xm.commons.metric;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import static java.util.Objects.nonNull;
import static org.springframework.boot.health.contributor.Health.Builder;

@Slf4j
@Component
@ConditionalOnProperty(value = "application.kafkaHealth.enabled", matchIfMissing = false)
public class KafkaHealthIndicator extends AbstractHealthIndicator {

    private final KafkaAdmin kafkaAdmin;
    private final Integer connectionTimeout;
    private final String systemTopic;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin,
                                @Value("${application.kafkaHealth.connectionTimeout:1000}")
                                Integer connectionTimeout,
                                @Value("${application.kafka-system-queue:'system_queue'}")
                                String systemTopic) {
        this.kafkaAdmin = kafkaAdmin;
        this.connectionTimeout = connectionTimeout;
        this.systemTopic = systemTopic;
    }

    @Override
    protected void doHealthCheck(Builder builder) {
        StopWatch executionTime = StopWatch.createStarted();
        DescribeTopicsOptions describeTopicsOptions = new DescribeTopicsOptions().timeoutMs(connectionTimeout);

        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(List.of(systemTopic), describeTopicsOptions);
            var topicDescriptionMap = describeTopicsResult.allTopicNames().get();
            boolean monitoringResult = nonNull(topicDescriptionMap);
            if (monitoringResult) {
                log.debug("Connection to kafka is {}, time: {}", monitoringResult, executionTime.getTime());
                builder.up();
            } else {
                log.error("Connection to kafka is {}, time: {}", monitoringResult, executionTime.getTime());
                builder.down();
            }
        } catch (Exception e) {
            log.error("Exception when try connect to kafka topic: {}, exception: {}, time: {}", systemTopic, e, executionTime.getTime());
            builder.down(e);
        }
    }
}
