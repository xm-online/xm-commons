package com.icthh.xm.commons.topic.config;

import static com.icthh.xm.commons.topic.util.MessageRetryDetailsUtils.delete;
import static com.icthh.xm.commons.topic.util.MessageRetryDetailsUtils.getUpdatedOrGenerateRetryDetails;
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

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;

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
