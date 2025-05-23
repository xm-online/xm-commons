package com.icthh.xm.commons.topic.config;

import static com.icthh.xm.commons.topic.message.MessageHandler.EXCEPTION_MESSAGE;
import static com.icthh.xm.commons.topic.message.MessageHandler.EXCEPTION_STACKTRACE;
import static com.icthh.xm.commons.topic.util.MessageRetryDetailsUtils.getRetryCounter;
import static com.icthh.xm.commons.topic.util.MessageRetryDetailsUtils.getTotalProcessingTime;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ISOLATION_LEVEL_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.METADATA_MAX_AGE_CONFIG;
import static org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE;

import com.icthh.xm.commons.logging.trace.TraceWrapper;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageHandler;
import com.icthh.xm.commons.topic.util.MessageRetryUtils;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@RequiredArgsConstructor
public class MessageListenerContainerBuilder {

    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public AbstractMessageListenerContainer build(String tenantKey,
                                                  TopicConfig topicConfig,
                                                  MessageHandler messageHandler,
                                                  TraceWrapper traceWrapper) {
        Map<String, Object> consumerConfig = buildConsumerConfig(topicConfig);
        DefaultKafkaConsumerFactory<String, String> kafkaConsumerFactory =
            new DefaultKafkaConsumerFactory<>(
                consumerConfig,
                new StringDeserializer(),
                new StringDeserializer());

        ContainerProperties containerProperties = new ContainerProperties(topicConfig.getTopicName());
        containerProperties.setObservationEnabled(true);
        containerProperties.setAckMode(MANUAL_IMMEDIATE);
        containerProperties.setMessageListener(new MessageListener(topicConfig, messageHandler, tenantKey, traceWrapper));

        ConcurrentMessageListenerContainer<String, String> container =
              new ConcurrentMessageListenerContainer<>(kafkaConsumerFactory, containerProperties);
        container.setCommonErrorHandler(buildErrorHandler(tenantKey, topicConfig));
        return container;
    }

    private CommonErrorHandler buildErrorHandler(String tenantKey, TopicConfig topicConfig) {
        FixedBackOff fixedBackOff = new FixedBackOff();
        Integer retriesCount = topicConfig.getRetriesCount();
        if (retriesCount != null && retriesCount > 0) {
            fixedBackOff.setMaxAttempts(retriesCount);
        }
        Long backOffPeriod = topicConfig.getBackOffPeriod();
        if (backOffPeriod != null) {
            fixedBackOff.setInterval(backOffPeriod);
        }

        DefaultErrorHandler defaultErrorHandler = StringUtils.isNotEmpty(topicConfig.getDeadLetterQueue())
            ? new DefaultErrorHandler(getDeadLetterPublishingRecoverer(tenantKey, topicConfig), fixedBackOff)
            : new DefaultErrorHandler(fixedBackOff);

        defaultErrorHandler.setCommitRecovered(true);
        return defaultErrorHandler;
    }

    private ConsumerRecordRecoverer getDeadLetterPublishingRecoverer(String tenantKey, TopicConfig topicConfig) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, (r, ex) -> recover(r, tenantKey, topicConfig));
        recoverer.addHeadersFunction((record, ex) -> {
            Throwable e = ex;
            if (e instanceof ListenerExecutionFailedException && e.getCause() != null) {
                e = e.getCause();
            }
            Headers additional = new RecordHeaders();
            additional.add(new RecordHeader(EXCEPTION_MESSAGE, e.toString().getBytes(UTF_8)));
            additional.add(new RecordHeader(EXCEPTION_STACKTRACE, getStackTrace(e).getBytes(UTF_8)));
            return additional;
        });
        return recoverer;
    }

    private TopicPartition recover(ConsumerRecord<?, ?> record, String tenantKey,
                                   TopicConfig topicConfig) {
        String rawBody = String.valueOf(record.value());
        String deadLetterQueue = topicConfig.getDeadLetterQueue();

        try {
            MessageRetryUtils.putRid(record, tenantKey, topicConfig.getTopicName());

            log.warn("send message to dead-letter [{}] due to retry count exceeded [{}], "
                    + "total processing time = {} ms, body = [{}]",
                deadLetterQueue, getRetryCounter(record), getTotalProcessingTime(record), rawBody);
            return new TopicPartition(deadLetterQueue, record.partition());

        } finally {
            MdcUtils.clear();
        }
    }

    private Map<String, Object> buildConsumerConfig(TopicConfig topicConfig) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);

        String groupIdFromConf = topicConfig.getGroupId();
        String groupId = StringUtils.isEmpty(groupIdFromConf) ? UUID.randomUUID().toString() : groupIdFromConf;
        props.put(GROUP_ID_CONFIG, groupId);
        props.put(ENABLE_AUTO_COMMIT_CONFIG, false);

        if (isNotBlank(topicConfig.getAutoOffsetReset())) {
            props.put(AUTO_OFFSET_RESET_CONFIG, topicConfig.getAutoOffsetReset());
        }

        if (isNotBlank(topicConfig.getMetadataMaxAge())) {
            props.put(METADATA_MAX_AGE_CONFIG, topicConfig.getMetadataMaxAge());
        }

        if (isNotBlank(topicConfig.getIsolationLevel())) {
            props.put(ISOLATION_LEVEL_CONFIG, topicConfig.getIsolationLevel());
        }

        if (topicConfig.getMaxPollInterval() != null) {
            props.put(MAX_POLL_INTERVAL_MS_CONFIG, topicConfig.getMaxPollInterval());
        }

        return Collections.unmodifiableMap(props);
    }
}
