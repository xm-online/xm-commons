package com.icthh.xm.commons.metric;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@EmbeddedKafka(
    partitions = 1,
    topics = { "test_topic1", "test_topic2", "test_topic3" }
)
public class KafkaMetricsUnitTest {

    @MockitoBean
    private KafkaAdmin kafkaAdmin;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    private Map<String, Object> mockConfig = new HashMap<>();

    @Test
    public void connectionToKafkaTopicsIsSuccess() {
        KafkaMetrics kafkaMetrics = initKafkaMetricSet();
        assertTrue(kafkaMetrics.connectionToKafkaTopicsIsSuccess());
    }

    @Test
    @SneakyThrows
    public void connectionToKafkaTopicsIsNotSuccess() {
        KafkaMetrics kafkaMetrics = initUnreachablePort();
        assertFalse(kafkaMetrics.connectionToKafkaTopicsIsSuccess());
    }

    @Test
    public void connectionToKafkaIsNotSuccessWithWrongTopic() {
        KafkaMetrics kafkaMetrics = initNotExistTopic();
        assertFalse(kafkaMetrics.connectionToKafkaTopicsIsSuccess());
    }

    private KafkaMetrics initKafkaMetricSet() {
        mockConfig.put("bootstrap.servers", embeddedKafka.getBrokersAsString());
        when(kafkaAdmin.getConfigurationProperties()).thenReturn(mockConfig);
        return new KafkaMetrics(kafkaAdmin, 1000, asList("test_topic1", "test_topic2"));
    }

    private KafkaMetrics initNotExistTopic() {
        mockConfig.put("bootstrap.servers", embeddedKafka.getBrokersAsString());
        when(kafkaAdmin.getConfigurationProperties()).thenReturn(mockConfig);
        return new KafkaMetrics(kafkaAdmin,
            1000,
            asList("test_topic1", "test_topic2", "test_topic6"));
    }

    private KafkaMetrics initUnreachablePort() {
        mockConfig.put("bootstrap.servers", "localhost:65530");
        mockConfig.put("request.timeout.ms", 200);
        mockConfig.put("default.api.timeout.ms", 200);
        mockConfig.put("retries", 0);
        mockConfig.put("retry.backoff.ms", 0);
        when(kafkaAdmin.getConfigurationProperties()).thenReturn(mockConfig);
        return new KafkaMetrics(kafkaAdmin,
            1,
            asList("test_topic1", "test_topic2", "test_topic6"));
    }
}
