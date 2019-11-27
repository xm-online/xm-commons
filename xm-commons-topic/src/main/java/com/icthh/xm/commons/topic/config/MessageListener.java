package com.icthh.xm.commons.topic.config;

import com.icthh.xm.commons.topic.message.MessageHandler;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;

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
        final StopWatch stopWatch = StopWatch.createStarted();
        String rawBody = record.value();
        log.info("start processing message for tenant: [{}], body = {}", tenantKey, rawBody);

        messageHandler.onMessage(rawBody, tenantKey, topicConfig);
        acknowledgment.acknowledge();

        log.info("stop processing message for tenant: [{}], time = {}", tenantKey, stopWatch.getTime());
    }
}
