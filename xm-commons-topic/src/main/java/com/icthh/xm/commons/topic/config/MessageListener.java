package com.icthh.xm.commons.topic.config;

import static com.icthh.xm.commons.topic.util.MessageHeaderUtils.getAndIncrementRetryCounter;
import static com.icthh.xm.commons.topic.util.MessageHeaderUtils.getOrGenerateRid;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigInteger;
import java.util.StringJoiner;

@Slf4j
public class MessageListener implements AcknowledgingMessageListener<String, String> {

    private final TopicConfig topicConfig;
    private final MessageHandler messageHandler;
    private final String tenantKey;

    public MessageListener(MessageHandler messageHandler, String tenantKey, TopicConfig topicConfig) {
        this.topicConfig = topicConfig;
        this.messageHandler = messageHandler;
        this.tenantKey = tenantKey;
    }

    @Override
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        BigInteger retryCount = getAndIncrementRetryCounter(record);
        String rid = getOrGenerateRid(record);
        putRid(retryCount, rid);

        final StopWatch stopWatch = StopWatch.createStarted();
        String rawBody = record.value();
        log.info("start processing message, size = {}, body = [{}]", rawBody.length(), formatBody(rawBody));

        try {
            messageHandler.onMessage(rawBody, tenantKey, topicConfig);
            acknowledgment.acknowledge();
            log.info("stop processing message, time = {} ms.", stopWatch.getTime());
        } catch (Exception ex) {
            log.error("error processing message, retry number: {}, time = {} ms.", retryCount, stopWatch.getTime());
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
