package com.icthh.xm.commons.metric;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class KafkaMetricsTest {

    @MockBean
    private KafkaAdmin kafkaAdmin;

    private static KafkaEmbedded kafkaEmbedded;

    private Map<String, Object> mockConfig = new HashMap<>();

    @Before
    @SneakyThrows
    public void setup() {
        if (Objects.isNull(kafkaEmbedded)) {
            kafkaEmbedded = new KafkaEmbedded(1,
                true,
                "test_topic1", "test_topic2", "test_topic3");
            kafkaEmbedded.setKafkaPorts(9092);
        }
        kafkaEmbedded.before();
    }

    @After
    public void after() {
        kafkaEmbedded.after();
    }

    @Test
    public void connectionToKafkaTopicsIsSuccess() {
        KafkaMetrics kafkaMetrics = initKafkaMetricSet();
        assertTrue(kafkaMetrics.connectionToKafkaTopicsIsSuccess());
    }

    @Test
    @SneakyThrows
    public void connectionToKafkaTopicsIsNotSuccess() {
        KafkaMetrics kafkaMetrics = initKafkaMetricSet();
        kafkaEmbedded.destroy();
        assertFalse(kafkaMetrics.connectionToKafkaTopicsIsSuccess());
    }

    @Test
    public void connectionToKafkaIsNotSuccessWithWrongTopic() {
        KafkaMetrics kafkaMetrics = initNotExistTopic();
        assertFalse(kafkaMetrics.connectionToKafkaTopicsIsSuccess());
    }

    private KafkaMetrics initKafkaMetricSet() {
        mockConfig.put("bootstrap.servers", "localhost:9092");
        when(kafkaAdmin.getConfigurationProperties()).thenReturn(mockConfig);
        return new KafkaMetrics(kafkaAdmin, 1000, asList("test_topic1", "test_topic2"));
    }

    private KafkaMetrics initNotExistTopic() {
        mockConfig.put("bootstrap.servers", "localhost:9092");
        when(kafkaAdmin.getConfigurationProperties()).thenReturn(mockConfig);
        return new KafkaMetrics(kafkaAdmin,
            1000,
            asList("test_topic1", "test_topic2", "test_topic6"));
    }
}

