package com.icthh.xm.commons.scheduler.metric;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaBinderConfigurationProperties;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Slf4j
@Component
@ConditionalOnProperty("application.scheduler-enabled")
@RequiredArgsConstructor
public class KafkaOffsetsMetric implements MetricSet {

    private static final String QUEUE = "_queue";
    private static final String DELIMITER = "_";
    private static final String METRIC_NAME = "kafka.offsets.";
    private static final String TOPIC_PREFIX = "scheduler_";

    @Value("${application.scheduler-config.kafka-offsets-metric-timeout:5}")
    private int timeout;

    @Value("${spring.application.name}")
    private String appName;

    private final TenantListRepository tenantListRepository;
    private final KafkaBinderConfigurationProperties binderConfigurationProperties;

    private ConsumerFactory<?, ?> defaultConsumerFactory;
    private Consumer<?, ?> consumer;

    @Getter
    @RequiredArgsConstructor
    private class Offsets {

        private final long totalLag;
        private final long totalCurrentOffset;
        private final long totalEndOffset;
    }

    private Offsets calculateConsumerOffsetsOnTopic(String topic, String group) {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<Offsets> future = exec.submit(() -> {

            long totalCurrentOffset = 0;
            long totalEndOffset = 0;

            try {
                if (consumer == null) {
                    synchronized (KafkaOffsetsMetric.this) {
                        if (consumer == null) {
                            consumer = createConsumerFactory(group).createConsumer();
                        }
                    }
                }
                synchronized (consumer) {
                    List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);
                    List<TopicPartition> topicPartitions = new LinkedList<>();
                    for (PartitionInfo partitionInfo : partitionInfos) {
                        topicPartitions.add(new TopicPartition(partitionInfo.topic(), partitionInfo.partition()));
                    }

                    Map<TopicPartition, Long> endOffsets = consumer.endOffsets(topicPartitions);

                    for (Map.Entry<TopicPartition, Long> endOffset : endOffsets.entrySet()) {
                        OffsetAndMetadata current = consumer.committed(endOffset.getKey());
                        if (current != null) {
                            totalEndOffset += endOffset.getValue();
                            totalCurrentOffset += current.offset();
                        } else {
                            totalEndOffset += endOffset.getValue();
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Cannot generate metric for topic: " + topic, e);
            }

            return new Offsets(totalEndOffset - totalCurrentOffset, totalCurrentOffset, totalEndOffset);
        });
        try {
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Offsets(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);
        } catch (ExecutionException | TimeoutException e) {
            return new Offsets(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);
        } finally {
            exec.shutdownNow();
        }
    }

    private ConsumerFactory<?, ?> createConsumerFactory(String group) {
        if (this.defaultConsumerFactory == null) {
            Map<String, Object> props = new HashMap<>();
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
            if (!ObjectUtils.isEmpty(binderConfigurationProperties.getConsumerProperties())) {
                props.putAll(binderConfigurationProperties.getConsumerProperties());
            }
            if (!props.containsKey(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)) {
                props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    this.binderConfigurationProperties.getKafkaConnectionString());
            }
            props.put("group.id", group);
            this.defaultConsumerFactory = new DefaultKafkaConsumerFactory<>(props);
        }

        return this.defaultConsumerFactory;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        Map<String, Metric> metrics = new HashMap<>();
        Set<String> tenants = tenantListRepository.getTenants();

        tenants.forEach(tenantName -> {
            String topic = TOPIC_PREFIX + tenantName.toLowerCase() + DELIMITER + appName  + QUEUE;
            metrics.put(METRIC_NAME + topic, (Gauge<Offsets>) () -> calculateConsumerOffsetsOnTopic(topic, appName));
        });

        return metrics;
    }
}
