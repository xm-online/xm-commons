package com.icthh.xm.commons.topic.config;

import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.springframework.kafka.listener.AbstractMessageListenerContainer.AckMode.MANUAL_IMMEDIATE;

import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageHandler;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.adapter.RetryingMessageListenerAdapter;
import org.springframework.kafka.listener.config.ContainerProperties;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class MessageListenerContainerBuilder {

    private final MessageHandler messageHandler;
    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public AbstractMessageListenerContainer build(String tenantKey, TopicConfig topicConfig) {
        Map<String, Object> consumerConfig = buildConsumerConfig(topicConfig);
        DefaultKafkaConsumerFactory<String, String> kafkaConsumerFactory =
            new DefaultKafkaConsumerFactory<>(
                consumerConfig,
                new StringDeserializer(),
                new StringDeserializer());

        ContainerProperties containerProperties = new ContainerProperties(topicConfig.getTopicName());
        containerProperties.setAckMode(MANUAL_IMMEDIATE);
        containerProperties.setMessageListener(new RetryingMessageListenerAdapter<>(
            new MessageListener(messageHandler, tenantKey, topicConfig),
            new MessageRetryTemplate(topicConfig),
            new ConsumerRecoveryCallback(tenantKey, topicConfig, kafkaTemplate)
        ));

        return new ConcurrentMessageListenerContainer<>(kafkaConsumerFactory, containerProperties);
    }

    private Map<String, Object> buildConsumerConfig(TopicConfig topicConfig) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();

        String groupIdFromConf = topicConfig.getGroupId();
        String groupId = StringUtils.isEmpty(groupIdFromConf) ? UUID.randomUUID().toString() : groupIdFromConf;
        props.put(GROUP_ID_CONFIG, groupId);
        props.put(ENABLE_AUTO_COMMIT_CONFIG, false);

        return Collections.unmodifiableMap(props);
    }
}
