package com.icthh.xm.commons.topic.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.icthh.xm.commons.logging.trace.TraceWrapper;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageHandler;
import java.util.Map;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;

@RunWith(MockitoJUnitRunner.class)
public class MessageListenerUnitTest {

    private static final String TOPIC = "test_topic";
    private static final long TIMESTAMP = 1_700_000_000_123L;

    @Mock
    private MessageHandler messageHandler;
    @Mock
    private TraceWrapper traceWrapper;
    @Mock
    private Acknowledgment acknowledgment;

    private MessageListener listener;

    @Before
    public void setUp() {
        doAnswer(invocation -> {
            invocation.getArgument(1, Runnable.class).run();
            return null;
        }).when(traceWrapper).runWithSpan(any(ConsumerRecord.class), any(Runnable.class));

        TopicConfig topicConfig = new TopicConfig();
        topicConfig.setTopicName(TOPIC);
        listener = new MessageListener(topicConfig, messageHandler, "test", traceWrapper);
    }

    @Test
    public void shouldPassRecordMetadataToHandler() {
        RecordHeaders kafkaHeaders = new RecordHeaders();
        kafkaHeaders.add("custom", "value".getBytes(UTF_8));

        listener.onMessage(record(kafkaHeaders), acknowledgment);

        Map<String, byte[]> headers = captureHeaders();
        assertThat(headerValue(headers, KafkaHeaders.RECEIVED_TIMESTAMP)).isEqualTo(String.valueOf(TIMESTAMP));
        assertThat(headerValue(headers, KafkaHeaders.RECEIVED_PARTITION)).isEqualTo("3");
        assertThat(headerValue(headers, KafkaHeaders.OFFSET)).isEqualTo("42");
        assertThat(headerValue(headers, "custom")).isEqualTo("value");
        verify(acknowledgment).acknowledge();
    }

    @Test
    public void shouldKeepKafkaHeaderWithSameNameAsRecordMetadata() {
        RecordHeaders kafkaHeaders = new RecordHeaders();
        kafkaHeaders.add(KafkaHeaders.RECEIVED_TIMESTAMP, "from-producer".getBytes(UTF_8));

        listener.onMessage(record(kafkaHeaders), acknowledgment);

        assertThat(headerValue(captureHeaders(), KafkaHeaders.RECEIVED_TIMESTAMP)).isEqualTo("from-producer");
    }

    private ConsumerRecord<String, String> record(RecordHeaders headers) {
        return new ConsumerRecord<>(TOPIC, 3, 42L, TIMESTAMP, TimestampType.CREATE_TIME, 0, 4, "key", "body",
            headers, Optional.empty());
    }

    @SuppressWarnings("unchecked")
    private Map<String, byte[]> captureHeaders() {
        ArgumentCaptor<Map<String, byte[]>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messageHandler).onMessage(eq("body"), eq("TEST"), any(TopicConfig.class), captor.capture());
        return captor.getValue();
    }

    private static String headerValue(Map<String, byte[]> headers, String name) {
        return new String(headers.get(name), UTF_8);
    }
}
