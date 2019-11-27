package com.icthh.xm.commons.topic;

import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.springframework.kafka.listener.AbstractMessageListenerContainer.AckMode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.topic.config.ConsumerRecoveryCallback;
import com.icthh.xm.commons.topic.config.MessageListener;
import com.icthh.xm.commons.topic.config.MessageRetryTemplate;
import com.icthh.xm.commons.topic.domain.ConsumerHolder;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.domain.TopicConsumersSpec;
import com.icthh.xm.commons.topic.message.MessageHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.adapter.RetryingMessageListenerAdapter;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicManager implements RefreshableConfiguration {

    private static final String CONSUMER_CONFIG_PATH_PATTERN = "/config/tenants/{tenant}/{ms}/topic-consumers.yml";
    private static final String TENANT_NAME = "tenant";

    private AntPathMatcher matcher = new AntPathMatcher();
    private ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());
    private Map<String, Map<String, ConsumerHolder>> topicConsumers = new ConcurrentHashMap<>();

    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MessageHandler messageHandler;

    @Override
    public void onRefresh(String updatedKey, String config) {
        refreshConfig(updatedKey, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(CONSUMER_CONFIG_PATH_PATTERN, updatedKey);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        if (isListeningConfiguration(configKey)) {
            refreshConfig(configKey, configValue);
        }
    }

    private void refreshConfig(String updatedKey, String config) {
        String tenantKey = extractTenant(updatedKey);
        Map<String, ConsumerHolder> existingConsumers = getTenantConsumers(tenantKey);

        if (StringUtils.isEmpty(config)) {
            stopAllTenantConsumers(updatedKey, existingConsumers);
            return;
        }
        TopicConsumersSpec spec = readSpec(updatedKey, config);

        //start and update consumers
        spec.getTopics().forEach(topicConfig -> processTopicConfig(tenantKey, topicConfig, existingConsumers));

        //remove old consumers
        //todo

        topicConsumers.put(tenantKey, existingConsumers);
    }

    private void processTopicConfig(String tenantKey,
                                    TopicConfig topicConfig,
                                    Map<String, ConsumerHolder> existingConsumers) {
        String topicConfigKey = topicConfig.getKey();
        ConsumerHolder existingConfig = existingConsumers.get(topicConfigKey);

        if (existingConfig == null) {
            startNewConsumer(tenantKey, topicConfig, existingConsumers);
            return;
        }

        if (existingConfig.getTopicConfig().equals(topicConfig)) {
            log.info("Consumer configuration: [{}] for tenant: [{}]"
                + " already exist and not chanced", topicConfig, tenantKey);
            return;
        }

        updateConsumer(tenantKey, topicConfig, existingConfig, existingConsumers);
    }

    @LoggingAspectConfig(inputExcludeParams = "existingConsumers")
    private void startNewConsumer(String tenantKey,
                                  TopicConfig topicConfig,
                                  Map<String, ConsumerHolder> existingConsumers) {
        AbstractMessageListenerContainer container = buildListenerContainer(tenantKey, topicConfig);
        container.start();

        existingConsumers.put(topicConfig.getKey(), new ConsumerHolder(topicConfig, container));
    }

    @LoggingAspectConfig(inputExcludeParams = {"existingConsumers", "existingConfig"})
    private void updateConsumer(String tenantKey,
                                TopicConfig topicConfig,
                                ConsumerHolder existingConfig,
                                Map<String, ConsumerHolder> existingConsumers) {
        existingConfig.getContainer().stop();

        AbstractMessageListenerContainer container = buildListenerContainer(tenantKey, topicConfig);
        container.start();

        existingConsumers.put(topicConfig.getKey(), new ConsumerHolder(topicConfig, container));
    }

    @LoggingAspectConfig
    private void stopAllTenantConsumers(String tenantKey,
                                        Map<String, ConsumerHolder> existingConsumers) {
        existingConsumers.values()
            .forEach(consumerHolder -> consumerHolder.getContainer().stop());

        topicConsumers.remove(tenantKey);
    }

    private AbstractMessageListenerContainer buildListenerContainer(String tenantKey, TopicConfig topicConfig) {
        Map<String, Object> consumerConfig = buildConsumerConfig(topicConfig);
        DefaultKafkaConsumerFactory<String, String> kafkaConsumerFactory =
            new DefaultKafkaConsumerFactory<>(
                consumerConfig,
                new StringDeserializer(),
                new StringDeserializer());

        ContainerProperties containerProperties = new ContainerProperties(topicConfig.getTopicName());
        containerProperties.setAckMode(AckMode.MANUAL_IMMEDIATE);
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

    private String extractTenant(final String updatedKey) {
        return matcher.extractUriTemplateVariables(CONSUMER_CONFIG_PATH_PATTERN, updatedKey).get(TENANT_NAME);
    }

    private TopicConsumersSpec readSpec(String updatedKey, String config) {
        TopicConsumersSpec spec = null;
        try {
            spec = ymlMapper.readValue(config, TopicConsumersSpec.class);
        } catch (Exception e) {
            log.error("Error read topic specification from path: {}", updatedKey, e);
        }
        return spec;
    }

    private Map<String, ConsumerHolder> getTenantConsumers(String tenantKey) {
        if (topicConsumers.containsKey(tenantKey)) {
            return topicConsumers.get(tenantKey);
        } else {
            return new ConcurrentHashMap<>();
        }
    }
}
