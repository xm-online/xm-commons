package com.icthh.xm.commons.topic;

import static java.lang.Thread.sleep;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.singleton;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.kafka.test.utils.KafkaTestUtils.consumerProps;
import static org.springframework.kafka.test.utils.KafkaTestUtils.producerProps;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.topic.message.MessageHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                classes = {KafkaAutoConfiguration.class})
@EmbeddedKafka(topics = "kafka-queue", partitions = 1)
public class MessageListenerIntTest {

    private static final String TOPIC = "kafka-test-queue";
    private static final String GROUP = "test";
    private static final String UPDATE_KEY = "/config/tenants/test/some-ms/topic-consumers.yml";
    private static final String TENANT_KEY = "test";
    private static final String APP_NAME = "some-ms";
    private static final String CONFIG = "topic-consumers-4.yml";
    private static final String TEST_MESSAGE = "test message";

    @Autowired
    private EmbeddedKafkaBroker kafkaEmbedded;

    @Autowired
    private KafkaProperties kafkaProperties;

    @MockBean
    private MessageHandler messageHandler;

    @SneakyThrows
    @Test
    public void testSuccessProcessMessageWhenPollTimeIsOver() {
        DefaultKafkaConsumerFactory<String, String> kafkaConsumerFactory =
              new DefaultKafkaConsumerFactory<>(consumerProps(GROUP, "false", kafkaEmbedded),
                    new StringDeserializer(),
                    new StringDeserializer());
        Consumer<String, String> consumer = kafkaConsumerFactory.createConsumer();
        consumer.subscribe(singleton(TOPIC));
        consumer.poll(0);
        consumer.close();

        DefaultKafkaProducerFactory<String, String> kafkaProducerFactory = new DefaultKafkaProducerFactory<>(
              producerProps(kafkaEmbedded),
              new StringSerializer(),
              new StringSerializer());

        Producer<String, String> producer = kafkaProducerFactory.createProducer();
        kafkaProperties.getProperties().put("max.poll.interval.ms", "1000");

        TopicManager topicManager = new TopicManager(APP_NAME,
                                                     kafkaProperties,
                                                     new KafkaTemplate<>(kafkaProducerFactory),
                                                     messageHandler);
        topicManager.onRefresh(UPDATE_KEY, readConfig(CONFIG));

        doAnswer(answer -> {
            sleep(500);
            throw new BusinessException("test");
        }).doAnswer(answer -> {
            sleep(500);
            throw new BusinessException("test");
        }).doAnswer(answer -> {
            sleep(500);
            return null;
        }).when(messageHandler).onMessage(any(), any(), any());

        producer.send(new ProducerRecord<>(TOPIC, "test-id", TEST_MESSAGE));
        producer.flush();

        Mockito.verify(messageHandler, timeout(2000).atLeast(3))
              .onMessage(eq(TEST_MESSAGE), eq(TENANT_KEY), any());
        Mockito.verify(messageHandler, after(2000).times(3))
              .onMessage(eq(TEST_MESSAGE), eq(TENANT_KEY), any());
        verifyNoMoreInteractions(messageHandler);

        producer.close();
    }

    @SneakyThrows
    private String readConfig(String name) {
        return IOUtils.toString(this.getClass().getResourceAsStream("/config/" + name), defaultCharset());
    }
}
