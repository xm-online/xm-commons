package com.icthh.xm.commons.topic.config;

import static com.icthh.xm.commons.topic.util.MessageRetryDetailsUtils.getRetryCounter;
import static com.icthh.xm.commons.topic.util.MessageRetryDetailsUtils.getRid;
import static com.icthh.xm.commons.topic.util.MessageRetryDetailsUtils.getTotalProcessingTime;
import static org.springframework.kafka.listener.adapter.RetryingMessageListenerAdapter.CONTEXT_ACKNOWLEDGMENT;
import static org.springframework.kafka.listener.adapter.RetryingMessageListenerAdapter.CONTEXT_RECORD;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryContext;

import java.util.StringJoiner;

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
            log.warn("Message skipped. Message record is null for context: {}", context);
            return null;
        }
        String rawBody = String.valueOf(record.value());
        String deadLetterQueue = topicConfig.getDeadLetterQueue();

        try {
            putRid(record);

            if (StringUtils.isEmpty(deadLetterQueue)) {
                log.info("Message skipped. Processing failed for tenant: [{}], body = {}", tenantKey, rawBody);
                acknowledge(rawBody, context);
                return null;
            }

            kafkaTemplate.send(deadLetterQueue, rawBody);
            acknowledge(rawBody, context);

            log.warn("send message to dead-letter [{}] due to retry count exceeded [{}], "
                    + "total processing time = {} ms, body = [{}]",
                deadLetterQueue, getRetryCounter(record), getTotalProcessingTime(record), rawBody);
        } finally {
            MdcUtils.clear();
        }
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

    private void putRid(ConsumerRecord<?, ?> record) {
        MdcUtils.putRid(new StringJoiner(":")
            .add(tenantKey)
            .add(topicConfig.getTopicName())
            .add(getRid(record))
            .toString());
    }
}
