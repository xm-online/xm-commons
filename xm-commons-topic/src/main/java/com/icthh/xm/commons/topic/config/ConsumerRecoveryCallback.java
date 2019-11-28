package com.icthh.xm.commons.topic.config;

import static org.springframework.kafka.listener.adapter.RetryingMessageListenerAdapter.CONTEXT_ACKNOWLEDGMENT;
import static org.springframework.kafka.listener.adapter.RetryingMessageListenerAdapter.CONTEXT_RECORD;

import com.icthh.xm.commons.topic.domain.TopicConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryContext;

@Slf4j
public class ConsumerRecoveryCallback implements RecoveryCallback<Object> {

    private final String tenantKey;
    private final TopicConfig topicConfig;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ConsumerRecoveryCallback(String tenantKey,
                                    TopicConfig topicConfig,
                                    KafkaTemplate<String, String> kafkaTemplate) {
        this.tenantKey = tenantKey;
        this.topicConfig = topicConfig;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Object recover(RetryContext context) {
        ConsumerRecord<?, ?> record = (ConsumerRecord) context.getAttribute(CONTEXT_RECORD);
        if (record == null) {
            log.warn("Message skipped. Message record is null for");
            return null;
        }
        String rawBody = String.valueOf(record.value());
        String deadLetterQueue = topicConfig.getDeadLetterQueue();

        if (StringUtils.isEmpty(deadLetterQueue)) {
            log.info("Message skipped. Processing failed for tenant: [{}], body = {}", tenantKey, rawBody);
            return null;
        }

        kafkaTemplate.send(deadLetterQueue, rawBody);
        acknowledge(rawBody, context);

        log.info("Message processing failed for tenant: [{}], body = [{}], "
            + " message was send to dead letter queue: [{}]", tenantKey, rawBody, deadLetterQueue);
        return null;
    }

    private void acknowledge(String rawBody, RetryContext context) {
        Acknowledgment acknowledgment = (Acknowledgment) context.getAttribute(CONTEXT_ACKNOWLEDGMENT);
        if (acknowledgment == null) {
            log.warn("Acknowledge failed for message: [{}], tenant: [{}]", rawBody, tenantKey);
            return;
        }
        acknowledgment.acknowledge();
    }
}
