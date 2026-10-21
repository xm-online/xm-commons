package com.icthh.xm.commons.topic.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.logging.trace.TraceWrapper;
import java.util.Map;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.KafkaHeaders;

public class KafkaTemplateServiceUnitTest {

    private final MockProducer<String, String> producer =
        new MockProducer<>(true, null, new StringSerializer(), new StringSerializer());
    private KafkaTemplateService kafkaTemplateService;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        ProducerFactory<String, String> producerFactory = mock(ProducerFactory.class);
        when(producerFactory.createProducer()).thenReturn(producer);
        kafkaTemplateService = new KafkaTemplateService(new KafkaTemplate<>(producerFactory), mock(TraceWrapper.class));
    }

    @Test
    public void headersDoNotOverrideTopicKeyAndPartition() {
        Map<String, Object> headers = Map.of(
            KafkaHeaders.TOPIC, "other_topic",
            KafkaHeaders.KEY, "other_key",
            KafkaHeaders.PARTITION, 5,
            "custom", "value");

        kafkaTemplateService.send("topic", "data", headers);

        ProducerRecord<String, String> record = producer.history().getFirst();
        assertEquals("topic", record.topic());
        assertNull(record.key());
        assertNull(record.partition());
        assertNotNull(record.headers().lastHeader("custom"));
    }

    @Test
    public void sendsToGivenTopicKeyAndPartition() {
        kafkaTemplateService.send("topic", 0, "key", "data", Map.of(KafkaHeaders.TOPIC, "other_topic"));

        ProducerRecord<String, String> record = producer.history().getFirst();
        assertEquals("topic", record.topic());
        assertEquals("key", record.key());
        assertEquals(Integer.valueOf(0), record.partition());
    }
}
