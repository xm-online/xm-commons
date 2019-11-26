package com.icthh.xm.commons.topic;

import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.springframework.kafka.listener.AbstractMessageListenerContainer.AckMode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.topic.message.MessageHandler;
import com.icthh.xm.commons.topic.spec.TopicConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.adapter.RetryingMessageListenerAdapter;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class MessageListenerContainerBuilder {

    private final KafkaProperties kafkaProperties;
    private final MessageHandler messageListenerHandler;

    public MessageListenerContainerBuilder(KafkaProperties kafkaProperties,
                                           MessageHandler messageListenerHandler) {
        this.kafkaProperties = kafkaProperties;
        this.messageListenerHandler = messageListenerHandler;
    }

    //todo partition?
    public AbstractMessageListenerContainer build(String tenantKey, TopicConfig topicConfig) {
        Map<String, Object> consumerConfig = buildConsumerConfig(topicConfig);
        DefaultKafkaConsumerFactory<String, String> kafkaConsumerFactory =
            new DefaultKafkaConsumerFactory<>(
                consumerConfig,
                new StringDeserializer(),
                new StringDeserializer());

        ContainerProperties containerProperties = new ContainerProperties(topicConfig.getTopicName());
        containerProperties.setAckMode(AckMode.MANUAL_IMMEDIATE);
        containerProperties.setMessageListener(new RetryingMessageListenerAdapter<>(
            buildMessageListener(tenantKey),
            buildRetryTemplate(topicConfig),
            buildRecoveryCallback()
        ));

        return new ConcurrentMessageListenerContainer<>(kafkaConsumerFactory, containerProperties);
    }

    //todo which format to use?
    private AcknowledgingMessageListener<String, String> buildMessageListener(final String tenantKey) {
        return (record, acknowledgment) -> {
            StopWatch stopWatch = StopWatch.createStarted();
            String rawBody = record.value();
            log.info("start processing message for tenant: [{}], body = {}", tenantKey, rawBody);
            Map map = jsonToMap(rawBody);

            messageListenerHandler.onMessage(map, tenantKey);
            acknowledgment.acknowledge();

            log.info("stop processing message for tenant: [{}], time = {}", tenantKey, stopWatch.getTime());
        };
    }

    private RetryTemplate buildRetryTemplate(final TopicConfig topicConfig) {
        RetryTemplate template = new RetryTemplate();

        Integer retriesCount = topicConfig.getRetriesCount();
        if (retriesCount == null || retriesCount.equals(-1)) {
            template.setRetryPolicy(new AlwaysRetryPolicy());
        } else {
            template.setRetryPolicy(new SimpleRetryPolicy(retriesCount));
        }

        Long backOffPeriod = topicConfig.getBackOffPeriod();
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        if (backOffPeriod != null) {
            fixedBackOffPolicy.setBackOffPeriod(backOffPeriod);
        }
        template.setBackOffPolicy(fixedBackOffPolicy);
        return template;
    }

    private RecoveryCallback<Object> buildRecoveryCallback() {
        return context -> {
            //TODO impl
            log.info("DEAD QUEUE!!!!!!!!!!!!!!!");
            return null;
        };
    }

    private Map<String, Object> buildConsumerConfig(TopicConfig topicConfig) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();

        String groupIdFromConf = topicConfig.getGroupId();
        String groupId = StringUtils.isEmpty(groupIdFromConf) ? UUID.randomUUID().toString() : groupIdFromConf;
        props.put(GROUP_ID_CONFIG, groupId);
        props.put(ENABLE_AUTO_COMMIT_CONFIG, false);

        return Collections.unmodifiableMap(props);
    }

    private Map jsonToMap(String recordValue) {
        Map message = null;
        try {
            message = new ObjectMapper().readValue(recordValue, Map.class);
        } catch (Exception e) {
            log.error("Error read json from string: {}", recordValue, e);
        }
        return message;
    }
}
