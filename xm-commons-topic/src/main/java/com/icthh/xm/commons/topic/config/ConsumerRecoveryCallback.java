package com.icthh.xm.commons.topic.config;

import static com.icthh.xm.commons.topic.message.MessageHandler.EXCEPTION_MESSAGE;
import static com.icthh.xm.commons.topic.message.MessageHandler.EXCEPTION_STACKTRACE;
import static com.icthh.xm.commons.topic.util.MessageRetryDetailsUtils.getRetryCounter;
import static com.icthh.xm.commons.topic.util.MessageRetryDetailsUtils.getRid;
import static com.icthh.xm.commons.topic.util.MessageRetryDetailsUtils.getTotalProcessingTime;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.springframework.kafka.listener.adapter.RetryingMessageListenerAdapter.CONTEXT_ACKNOWLEDGMENT;
import static org.springframework.kafka.listener.adapter.RetryingMessageListenerAdapter.CONTEXT_RECORD;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
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

            Headers headers = addExceptionHeaders(context, record);
            ProducerRecord<String, String> dlqRecord = new ProducerRecord<>(
                deadLetterQueue,
                record.partition(),
                record.key() != null ? record.key().toString() : null,
                rawBody,
                headers
            );
            kafkaTemplate.send(dlqRecord);

            acknowledge(rawBody, context);

            log.warn("send message to dead-letter [{}] due to retry count exceeded [{}], "
                    + "total processing time = {} ms, body = [{}]",
                deadLetterQueue, getRetryCounter(record), getTotalProcessingTime(record), rawBody);
        } finally {
            MdcUtils.clear();
        }
        return null;
    }

    private static Headers addExceptionHeaders(RetryContext context, ConsumerRecord<?, ?> record) {
        Headers headers = new RecordHeaders(record.headers());
        Throwable e = context.getLastThrowable();
        if (e instanceof ListenerExecutionFailedException && e.getCause() != null) {
            e = e.getCause();
        }
        headers.add(new RecordHeader(EXCEPTION_MESSAGE, e.toString().getBytes(UTF_8)));
        headers.add(new RecordHeader(EXCEPTION_STACKTRACE, getStackTrace(e).getBytes(UTF_8)));
        return headers;
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
