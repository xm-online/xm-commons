package com.icthh.xm.commons.topic;

import static java.lang.Thread.sleep;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.singleton;
import static org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.TRANSACTIONAL_ID_CONFIG;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import java.util.Map;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                classes = {KafkaAutoConfiguration.class})
@EmbeddedKafka(topics = "kafka-queue", partitions = 1,
    brokerProperties = {
    "transaction.state.log.replication.factor=1",
    "offsets.topic.replication.facto=1",
    "transaction.state.log.min.isr=1"
    })
public class MessageListenerIntTest {

    private static final String TOPIC = "kafka-test-queue";
    private static final String GROUP = "test";
    private static final String UPDATE_KEY = "/config/tenants/test/some-ms/topic-consumers.yml";
    private static final String TENANT_KEY = "test";
    private static final String APP_NAME = "some-ms";
    private static final String CONFIG = "topic-consumers-4.yml";
    private static final String TX_CONFIG = "topic-consumers-without-isolation.yml";
    private static final String TX_RC_CONFIG = "topic-consumers-with-isolation-read_committed.yml";
    private static final String TEST_MESSAGE = "test message";

    @Autowired
    private EmbeddedKafkaBroker kafkaEmbedded;

    @Autowired
    private KafkaProperties kafkaProperties;

    private MessageHandler messageHandler;

    @Before
    public void before() {
        messageHandler = mock(MessageHandler.class);
    }

    @SneakyThrows
    @Test
    public void testSuccessProcessMessageWhenPollTimeIsOver() {
        initConsumers();

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
        Thread.sleep(50); // for rebalance cluster, and avoid message will be consumed by other consumer

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

        verify(messageHandler, timeout(2000).atLeast(3))
              .onMessage(eq(TEST_MESSAGE), eq(TENANT_KEY), any());
        verify(messageHandler, after(2000).times(3))
              .onMessage(eq(TEST_MESSAGE), eq(TENANT_KEY), any());
        verifyNoMoreInteractions(messageHandler);

        producer.close();

        topicManager.onRefresh(UPDATE_KEY, "");
    }

    @SneakyThrows
    private String readConfig(String name) {
        return IOUtils.toString(this.getClass().getResourceAsStream("/config/" + name), defaultCharset());
    }

    @SneakyThrows
    @Test
    public void testConsumerReadUncommited() {
        initConsumers();

        TopicManager topicManager = new TopicManager(APP_NAME,
            kafkaProperties,
            null,
            messageHandler);
        topicManager.onRefresh(UPDATE_KEY, readConfig(TX_CONFIG));

        Producer<String, String> producer = createTxProducer();
        producer.initTransactions();
        producer.beginTransaction();
        producer.send(new ProducerRecord<>("kafka-tx-queue", "value1"));
        producer.flush();

        verify(messageHandler, timeout(2000).atLeastOnce())
            .onMessage(eq("value1"), eq(TENANT_KEY), any());

        producer.send(new ProducerRecord<>("kafka-tx-queue", "value2"));
        producer.flush();

        verify(messageHandler, timeout(2000).atLeastOnce())
            .onMessage(eq("value2"), eq(TENANT_KEY), any());

        producer.commitTransaction();

        producer.close();

        topicManager.onRefresh(UPDATE_KEY, "");
    }

    @Test
    @SneakyThrows
    public void testConsumerReadCommitted() {
        initConsumers();

        TopicManager topicManager = new TopicManager(APP_NAME,
            kafkaProperties,
            null,
            messageHandler);
        topicManager.onRefresh(UPDATE_KEY, readConfig(TX_RC_CONFIG));

        Producer<String, String> producer = createTxProducer();
        producer.initTransactions();
        producer.beginTransaction();
        producer.send(new ProducerRecord<>("kafka-tx-queue", "value1"));
        producer.flush();

        Thread.sleep(1000);
        verifyZeroInteractions(messageHandler);

        producer.send(new ProducerRecord<>("kafka-tx-queue", "value2"));
        producer.flush();

        Thread.sleep(1000);
        verifyZeroInteractions(messageHandler);

        producer.commitTransaction();

        verify(messageHandler, timeout(2000)).onMessage(eq("value1"), eq(TENANT_KEY), any());
        verify(messageHandler, timeout(2000)).onMessage(eq("value2"), eq(TENANT_KEY), any());

        producer.close();

        topicManager.onRefresh(UPDATE_KEY, "");
    }

    private void initConsumers() {
        DefaultKafkaConsumerFactory<String, String> kafkaConsumerFactory =
            new DefaultKafkaConsumerFactory<>(consumerProps(GROUP, "false", kafkaEmbedded),
                new StringDeserializer(),
                new StringDeserializer());
        Consumer<String, String> consumer = kafkaConsumerFactory.createConsumer();
        consumer.subscribe(singleton(TOPIC));
        consumer.poll(0);
        consumer.close();
    }


    private Producer<String, String> createTxProducer() {
        Map<String, Object> producerProps = producerProps(kafkaEmbedded);
        producerProps.put(TRANSACTIONAL_ID_CONFIG, "txId" + System.currentTimeMillis());
        producerProps.put(RETRIES_CONFIG, 1);
        producerProps.put("", 1);
        DefaultKafkaProducerFactory<String, String> kafkaProducerFactory = new DefaultKafkaProducerFactory<>(
            producerProps,
            new StringSerializer(),
            new StringSerializer());

        return kafkaProducerFactory.createProducer();
    }
}
