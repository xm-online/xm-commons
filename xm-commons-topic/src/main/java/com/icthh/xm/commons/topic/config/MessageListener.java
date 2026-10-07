package com.icthh.xm.commons.topic.config;

import static com.icthh.xm.commons.topic.util.MessageRetryDetailsUtils.delete;
import static com.icthh.xm.commons.topic.util.MessageRetryDetailsUtils.getUpdatedOrGenerateRetryDetails;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;

import com.icthh.xm.commons.logging.trace.TraceWrapper;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageHandler;
import com.icthh.xm.commons.topic.util.MessageRetryDetailsUtils.MessageRetryDetails;
import java.math.BigInteger;
import java.util.Map;
import java.util.StringJoiner;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;

@Slf4j
public class MessageListener implements AcknowledgingMessageListener<String, String> {

    private final TopicConfig topicConfig;
    private final MessageHandler messageHandler;
    private final String tenantKey;
    private final TraceWrapper traceWrapper;

    public MessageListener(TopicConfig topicConfig, MessageHandler messageHandler, String tenantKey,
                           TraceWrapper traceWrapper) {
        this.topicConfig = topicConfig;
        this.messageHandler = messageHandler;
        this.tenantKey = tenantKey.toUpperCase();
        this.traceWrapper = traceWrapper;
    }

    @Override
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        traceWrapper.runWithSpan(record, () -> processMessage(record, acknowledgment));
    }

    private void processMessage(ConsumerRecord<String, String> record,
                                Acknowledgment acknowledgment) {
        MessageRetryDetails retryDetails = getUpdatedOrGenerateRetryDetails(record);
        putRid(retryDetails.getRetryCount(), retryDetails.getRid());
        final StopWatch stopWatch = StopWatch.createStarted();
        String rawBody = record.value();
        log.info("start processing message, size = {}, body = [{}]", rawBody.length(), formatBody(rawBody));
        try {
            Map<String, byte[]> headers = stream(record.headers().spliterator(), false).collect(toMap(Header::key, Header::value));
            addRecordMetadata(headers, record);
            messageHandler.onMessage(rawBody, tenantKey, topicConfig, headers);
            acknowledgment.acknowledge();
            delete(record);
            log.info("stop processing message, time = {} ms.", stopWatch.getTime());
        } catch (Exception ex) {
            log.error("error processing message, retry number: {}, time = {} ms.", retryDetails.getRetryCount(),
                stopWatch.getTime());
            throw ex;
        } finally {
            MdcUtils.clear();
        }
    }

    /**
     * Record metadata is not part of the Kafka headers, so expose it to the handler the same way
     * spring-kafka does for {@code Message<?>} listeners. Values are decimal strings in UTF-8.
     * A real Kafka header with the same name takes precedence.
     */
    private static void addRecordMetadata(Map<String, byte[]> headers, ConsumerRecord<String, String> record) {
        headers.putIfAbsent(KafkaHeaders.RECEIVED_TIMESTAMP, toBytes(record.timestamp()));
        headers.putIfAbsent(KafkaHeaders.RECEIVED_PARTITION, toBytes(record.partition()));
        headers.putIfAbsent(KafkaHeaders.OFFSET, toBytes(record.offset()));
    }

    private static byte[] toBytes(long value) {
        return String.valueOf(value).getBytes(UTF_8);
    }

    private void putRid(BigInteger retryCount, String rid) {
        MdcUtils.putRid(new StringJoiner(":")
            .add(tenantKey)
            .add(topicConfig.getTopicName())
            .add(rid)
            .add(retryCount.toString())
            .toString());
    }

    private String formatBody(String rawBody) {
        return topicConfig.getLogBody() ? rawBody : "***";
    }
}
