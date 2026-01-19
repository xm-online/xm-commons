package com.icthh.xm.commons.topic;

import static com.icthh.xm.commons.topic.message.MessageHandler.EXCEPTION_MESSAGE;
import static java.lang.Thread.sleep;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.singleton;
import static org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.TRANSACTIONAL_ID_CONFIG;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.kafka.test.utils.KafkaTestUtils.consumerProps;
import static org.springframework.kafka.test.utils.KafkaTestUtils.producerProps;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.logging.trace.TraceWrapper;
import com.icthh.xm.commons.topic.config.TestBeanConfiguration;
import com.icthh.xm.commons.topic.domain.NotRetryableException;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageHandler;
import com.icthh.xm.commons.topic.service.TopicConfigurationService;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                classes = {KafkaAutoConfiguration.class})
@EmbeddedKafka(topics = "kafka-queue", partitions = 1, controlledShutdown = true,
    brokerProperties = {
    "transaction.state.log.replication.factor=1",
    "offsets.topic.replication.facto=1",
    "transaction.state.log.min.isr=1"
    })
@ContextConfiguration(classes = TestBeanConfiguration.class)
public class MessageListenerIntTest {

    private static final String TOPIC_FOR_TEST_DEAD_LETTER = "dead-letter-test";
    private static final String TOPIC_DEAD_QUEUE = "kafka-test-dead-queue";

    private static final String TOPIC = "kafka-test-queue";
    private static final String GROUP = "test";
    private static final String TENANT_KEY = "TEST";
    private static final String UPDATE_KEY = "/config/tenants/" + TENANT_KEY + "/some-ms/topic-consumers.yml";
    private static final String CONFIG = "topic-consumers-4.yml";
    private static final String TX_CONFIG = "topic-consumers-without-isolation.yml";
    private static final String TX_RC_CONFIG = "topic-consumers-with-isolation-read_committed.yml";
    private static final String DEAD_LETTER_CONFIG = "topic-consumers-dead-letter.yml";
    private static final String TEST_MESSAGE = "test message";

    @Autowired
    private EmbeddedKafkaBroker kafkaEmbedded;

    @Autowired
    private KafkaProperties kafkaProperties;

    @Autowired
    private TraceWrapper traceWrapper;

    @Autowired
    private MessageHandler messageHandler;

    @Autowired
    private TopicConfigurationService topicConfigurationService;


    @BeforeEach
    public void before() {
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return null;
        }).when(traceWrapper).runWithSpan(any(ConsumerRecord.class), any(Runnable.class));
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return null;
        }).when(traceWrapper).runWithSpan(any(Message.class), any(MessageChannel.class), any(Runnable.class));
    }

    @BeforeEach
    public void clearMessageHandlerInvocations() {
        clearInvocations(messageHandler);
    }

    @SneakyThrows
    @Test
    public void testSuccessProcessMessageWhenPollTimeIsOver() {
        log.info("TRYFIXTEST init consumers");
        initConsumers(singleton(TOPIC));

        DefaultKafkaProducerFactory<String, String> kafkaProducerFactory = new DefaultKafkaProducerFactory<>(
              producerProps(kafkaEmbedded),
              new StringSerializer(),
              new StringSerializer());

        Producer<String, String> producer = kafkaProducerFactory.createProducer();
        kafkaProperties.getProperties().put("max.poll.interval.ms", "1000");

        topicConfigurationService.onRefresh(UPDATE_KEY, readConfig(CONFIG));
        log.info("TRYFIXTEST Thread.sleep(50);");
        Thread.sleep(50); // for rebalance cluster, and avoid message will be consumed by other consumer

        doAnswer(answer -> {
            sleep(500);
            log.info("TRYFIXTEST slept 500 1");
            throw new BusinessException("test");
        }).doAnswer(answer -> {
            sleep(500);
            log.info("TRYFIXTEST slept 500 2");
            throw new BusinessException("test");
        }).doAnswer(answer -> {
            sleep(500);
            log.info("TRYFIXTEST slept 500 3");
            return null;
        }).when(messageHandler).onMessage(any(), any(), any(), any());

        producer.send(new ProducerRecord<>(TOPIC, "test-id", TEST_MESSAGE));
        producer.flush();
        log.info("TRYFIXTEST producer.flush();");

        verify(messageHandler, timeout(2000).atLeast(3))
              .onMessage(eq(TEST_MESSAGE), eq(TENANT_KEY), any(), any());
        verify(messageHandler, after(2000).times(3))
              .onMessage(eq(TEST_MESSAGE), eq(TENANT_KEY), any(), any());
        verifyNoMoreInteractions(messageHandler);

        producer.close();

        topicConfigurationService.onRefresh(UPDATE_KEY, "");
    }

    @SneakyThrows
    private String readConfig(String name) {
        return IOUtils.toString(this.getClass().getResourceAsStream("/config/" + name), defaultCharset());
    }

    @SneakyThrows
    @Test
    public void testConsumerReadUncommited() {
        initConsumers(singleton(TOPIC));

        topicConfigurationService.onRefresh(UPDATE_KEY, readConfig(TX_CONFIG));

        Producer<String, String> producer = createTxProducer();
        producer.beginTransaction();
        producer.send(new ProducerRecord<>("kafka-tx-queue", "value1"));
        producer.flush();

        verify(messageHandler, timeout(2000).atLeastOnce())
            .onMessage(eq("value1"), eq(TENANT_KEY), any(), any());

        producer.send(new ProducerRecord<>("kafka-tx-queue", "value2"));
        producer.flush();

        verify(messageHandler, timeout(2000).atLeastOnce())
            .onMessage(eq("value2"), eq(TENANT_KEY), any(), any());

        producer.commitTransaction();

        producer.close();

        topicConfigurationService.onRefresh(UPDATE_KEY, "");
    }

    @Test
    @SneakyThrows
    public void testConsumerReadCommitted() {
        initConsumers(singleton(TOPIC));

        topicConfigurationService.onRefresh(UPDATE_KEY, readConfig(TX_RC_CONFIG));

        Producer<String, String> producer = createTxProducer();
        producer.beginTransaction();
        producer.send(new ProducerRecord<>("kafka-tx-queue", "value1"));
        producer.flush();

        Thread.sleep(1000);
        verifyNoInteractions(messageHandler);

        producer.send(new ProducerRecord<>("kafka-tx-queue", "value2"));
        producer.flush();

        Thread.sleep(1000);
        verifyNoInteractions(messageHandler);

        producer.commitTransaction();
        verify(messageHandler, timeout(2000)).onMessage(eq("value1"), eq(TENANT_KEY), any(), any());
        verify(messageHandler, timeout(2000)).onMessage(eq("value2"), eq(TENANT_KEY), any(), any());
        producer.close();
        topicConfigurationService.onRefresh(UPDATE_KEY, "");
    }

    @Test
    @SneakyThrows
    public void testConsumerDeadLetter() {
        initConsumers(Set.of(TOPIC_FOR_TEST_DEAD_LETTER, TOPIC_DEAD_QUEUE));

        topicConfigurationService.onRefresh(UPDATE_KEY, readConfig(DEAD_LETTER_CONFIG));

        var config = deadLetterTestConsumerConfig();

        var configDeadLetter = deadLetterTestDealLetterConfig();

        doAnswer(answer -> {
            log.info("TEST dead letter exception");
            throw new BusinessException("test");
        }).when(messageHandler).onMessage(any(), any(), refEq(config), any());
        doAnswer(answer -> {
            log.info("TEST dead letter handling");
            return null;
        }).when(messageHandler).onMessage(any(), any(), refEq(configDeadLetter), any());

        Producer<String, String> producer = createTxProducer();
        producer.beginTransaction();
        producer.send(new ProducerRecord<>(TOPIC_FOR_TEST_DEAD_LETTER, "test value"));
        producer.flush();

        Thread.sleep(5000);

        InOrder inOrder = inOrder(messageHandler);
        inOrder.verify(messageHandler, times(3)).onMessage(eq("test value"), eq(TENANT_KEY), refEq(config), any());

        ArgumentCaptor<Map<String, byte[]>> headersCaptor = ArgumentCaptor.forClass(Map.class);
        inOrder.verify(messageHandler).onMessage(eq("test value"), eq(TENANT_KEY), refEq(configDeadLetter), headersCaptor.capture());
        Map<String, byte[]> headers = headersCaptor.getValue();
        assertEquals("{code=error.business, message=test}", new String(headers.get(EXCEPTION_MESSAGE)));
        verifyNoMoreInteractions(messageHandler);

        producer.close();

        topicConfigurationService.onRefresh(UPDATE_KEY, "");
    }

    @Test
    @SneakyThrows
    public void testNotRetryableException() {
        initConsumers(Set.of(TOPIC_FOR_TEST_DEAD_LETTER, TOPIC_DEAD_QUEUE));

        topicConfigurationService.onRefresh(UPDATE_KEY, readConfig(DEAD_LETTER_CONFIG));

        var config = deadLetterTestConsumerConfig();
        var configDeadLetter = deadLetterTestDealLetterConfig();

        doAnswer(answer -> {
            log.info("TEST dead letter exception");
            throw new NotRetryableException("test error message");
        }).when(messageHandler).onMessage(any(), any(), refEq(config), any());
        doAnswer(answer -> {
            log.info("TEST dead letter handling");
            return null;
        }).when(messageHandler).onMessage(any(), any(), refEq(configDeadLetter), any());

        Producer<String, String> producer = createTxProducer();
        producer.beginTransaction();
        producer.send(new ProducerRecord<>(TOPIC_FOR_TEST_DEAD_LETTER, "test value"));
        producer.flush();

        Thread.sleep(1000);

        InOrder inOrder = inOrder(messageHandler);
        // only once because message not retryable
        inOrder.verify(messageHandler, times(1)).onMessage(eq("test value"), eq(TENANT_KEY), refEq(config), any());

        ArgumentCaptor<Map<String, byte[]>> headersCaptor = ArgumentCaptor.forClass(Map.class);
        inOrder.verify(messageHandler).onMessage(eq("test value"), eq(TENANT_KEY), refEq(configDeadLetter), headersCaptor.capture());
        Map<String, byte[]> headers = headersCaptor.getValue();
        assertEquals("test error message", new String(headers.get(EXCEPTION_MESSAGE)));
        verifyNoMoreInteractions(messageHandler);

        producer.close();

        topicConfigurationService.onRefresh(UPDATE_KEY, "");
    }

    private static TopicConfig deadLetterTestDealLetterConfig() {
        var configDeadLetter = new TopicConfig();
        configDeadLetter.setKey("kafka-test-dead-queue");
        configDeadLetter.setTypeKey("kafka-test-dead-queue");
        configDeadLetter.setTopicName("kafka-test-dead-queue");
        configDeadLetter.setRetriesCount(1);
        configDeadLetter.setBackOffPeriod(1L);
        configDeadLetter.setDeadLetterQueue("kafka-test-absolute-dead-queue");
        return configDeadLetter;
    }

    private static TopicConfig deadLetterTestConsumerConfig() {
        var config = new TopicConfig();
        config.setKey("dead-letter-test");
        config.setTypeKey("dead-letter-test");
        config.setTopicName("dead-letter-test");
        config.setRetriesCount(2);
        config.setBackOffPeriod(1L);
        config.setDeadLetterQueue("kafka-test-dead-queue");
        return config;
    }


    private void initConsumers(Set<String> topics) {
        DefaultKafkaConsumerFactory<String, String> kafkaConsumerFactory =
            new DefaultKafkaConsumerFactory<>(consumerProps(kafkaEmbedded, GROUP, false),
                new StringDeserializer(),
                new StringDeserializer());
        Consumer<String, String> consumer = kafkaConsumerFactory.createConsumer();
        consumer.subscribe(topics);
        consumer.poll(Duration.ZERO);
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
