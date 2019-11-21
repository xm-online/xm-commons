package com.icthh.xm.commons.scheduler.service;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.kafka.test.utils.KafkaTestUtils.consumerProps;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getRecords;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;
import static org.springframework.kafka.test.utils.KafkaTestUtils.producerProps;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.codahale.metrics.Gauge;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.scheduler.metric.KafkaOffsetsMetric;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaBinderConfigurationProperties;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@EmbeddedKafka
public class KafkaOffsetsMetricTest {

    private static final String TOPIC = "scheduler_xm_test_queue";
    private static final String GROUP = "test";
    private static final String METRIC_NAME = "kafka.offsets.scheduler_xm_test_queue";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @MockBean
    private KafkaBinderConfigurationProperties kafkaBinderConfigurationProperties;

    @MockBean
    private TenantListRepository tenantListRepository;

    private KafkaOffsetsMetric kafkaOffsetsMetric;
    private Consumer<String, String> consumer;
    private Producer<String, String> producer;

    @Before
    public void setUp() {
        Map<String, Object> consumerConf = new HashMap<>(consumerProps(GROUP, "false", embeddedKafka));
        DefaultKafkaConsumerFactory<String, String> kafkaConsumerFactory = new DefaultKafkaConsumerFactory<>(
            consumerConf,
            new StringDeserializer(),
            new StringDeserializer());

        consumer = kafkaConsumerFactory.createConsumer();
        consumer.subscribe(singleton(TOPIC));
        consumer.poll(0);

        Map<String, Object> producerConf = new HashMap<>(producerProps(embeddedKafka));
        DefaultKafkaProducerFactory<String, String> kafkaProducerFactory = new DefaultKafkaProducerFactory<>(
            producerConf,
            new StringSerializer(),
            new StringSerializer());

        producer = kafkaProducerFactory.createProducer();

        when(tenantListRepository.getTenants()).thenReturn(Collections.singleton("xm"));
        kafkaOffsetsMetric = new KafkaOffsetsMetric(tenantListRepository, kafkaBinderConfigurationProperties);
        setField(kafkaOffsetsMetric, "consumer", kafkaConsumerFactory.createConsumer());
        setField(kafkaOffsetsMetric, "timeout", 3);
        setField(kafkaOffsetsMetric, "appName", GROUP);
    }

    @Test
    public void testGetKafkaOffsetsMetric() {
        producer.send(new ProducerRecord<>(TOPIC, "test-id", "{\"event\":\"Test Event\"}"));
        producer.flush();

        Object metricValue = ((Gauge) kafkaOffsetsMetric.getMetrics().get(METRIC_NAME)).getValue();
        assertEquals(1L, getField(metricValue, "totalLag"));
        assertEquals(0L, getField(metricValue, "totalCurrentOffset"));
        assertEquals(1L, getField(metricValue, "totalEndOffset"));

        ConsumerRecord<String, String> singleRecord = getSingleRecord(consumer, TOPIC);
        consumer.commitAsync();
        assertThat(singleRecord.key()).isEqualTo("test-id");
        assertThat(singleRecord.value()).isEqualTo("{\"event\":\"Test Event\"}");

        metricValue = ((Gauge) kafkaOffsetsMetric.getMetrics().get(METRIC_NAME)).getValue();
        assertEquals(0L, getField(metricValue, "totalLag"));
        assertEquals(1L, getField(metricValue, "totalCurrentOffset"));
        assertEquals(1L, getField(metricValue, "totalEndOffset"));

        producer.send(new ProducerRecord<>(TOPIC, "test-id", "{\"event\":\"Test Event\"}"));
        producer.send(new ProducerRecord<>(TOPIC, "test-id", "{\"event\":\"Test Event\"}"));
        producer.flush();

        metricValue = ((Gauge) kafkaOffsetsMetric.getMetrics().get(METRIC_NAME)).getValue();
        assertEquals(2L, getField(metricValue, "totalLag"));
        assertEquals(1L, getField(metricValue, "totalCurrentOffset"));
        assertEquals(3L, getField(metricValue, "totalEndOffset"));

        getRecords(consumer);
        consumer.commitAsync();

        metricValue = ((Gauge) kafkaOffsetsMetric.getMetrics().get(METRIC_NAME)).getValue();
        assertEquals(0L, getField(metricValue, "totalLag"));
        assertEquals(3L, getField(metricValue, "totalCurrentOffset"));
        assertEquals(3L, getField(metricValue, "totalEndOffset"));

        consumer.close();

    }
}
