package com.icthh.xm.commons.metric;

import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class KafkaMetricsSetTest {

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
        KafkaMetricsSet kafkaMetricsSet = initKafkaMetricSet();
        assertTrue(kafkaMetricsSet.connectionToKafkaTopicsIsSuccess());
    }

    @Test
    @SneakyThrows
    public void connectionToKafkaTopicsIsNotSuccess() {
        KafkaMetricsSet kafkaMetricsSet = initKafkaMetricSet();
        kafkaEmbedded.destroy();
        assertFalse(kafkaMetricsSet.connectionToKafkaTopicsIsSuccess());
    }

    @Test
    public void connectionToKafkaIsNotSuccessWithWrongTopic() {
        KafkaMetricsSet kafkaMetricsSet = initNotExistTopic();
        assertFalse(kafkaMetricsSet.connectionToKafkaTopicsIsSuccess());
    }

    private KafkaMetricsSet initKafkaMetricSet() {
        mockConfig.put("bootstrap.servers", "localhost:9092");
        when(kafkaAdmin.getConfig()).thenReturn(mockConfig);
        return new KafkaMetricsSet(kafkaAdmin, 1000, asList("test_topic1", "test_topic2"));
    }

    private KafkaMetricsSet initNotExistTopic() {
        mockConfig.put("bootstrap.servers", "localhost:9092");
        when(kafkaAdmin.getConfig()).thenReturn(mockConfig);
        return new KafkaMetricsSet(kafkaAdmin,
            1000,
            asList("test_topic1", "test_topic2", "test_topic6"));
    }
}
