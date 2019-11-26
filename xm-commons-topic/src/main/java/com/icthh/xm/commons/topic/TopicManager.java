package com.icthh.xm.commons.topic;

import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.topic.spec.TopicConfig;
import com.icthh.xm.commons.topic.spec.TopicConsumersSpec;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Import(KafkaProperties.class)
public class TopicManager implements RefreshableConfiguration {

    private static final String CONSUMER_CONFIG_PATH_PATTERN = "/config/tenants/{tenant}/{ms}/topic-consumers.yml";
    private static final String TENANT_NAME = "tenant";

    private AntPathMatcher matcher = new AntPathMatcher();
    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private final KafkaProperties kafkaProperties;

    public TopicManager(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Getter
    private Map<String, TopicConsumersSpec> topicConsumers = new ConcurrentHashMap<>();

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
        try {
            String tenant = extractTenant(updatedKey);
            if (StringUtils.isBlank(config)) {
                topicConsumers.remove(tenant);

                //log.info("Tasks for tenant '{}' were removed: {}", tenant, updatedKey);
            } else {
                TopicConsumersSpec spec = mapper.readValue(config, TopicConsumersSpec.class);
                topicConsumers.put(tenant, spec);

                initConsumers(spec);
                //log.info("Tasks for tenant '{}' were updated: {}", tenant, updatedKey);
            }
        } catch (Exception e) {
            log.error("Error read topic specification from path: {}", updatedKey, e);
        }
    }

    private String extractTenant(final String updatedKey) {
        return matcher.extractUriTemplateVariables(CONSUMER_CONFIG_PATH_PATTERN, updatedKey).get(TENANT_NAME);
    }

    private List<Object> initConsumers(TopicConsumersSpec spec) {
        spec.getTopics().forEach(topicConfig -> {

            Map<String, Object> consumerConfig = buildConsumerConfig(topicConfig);
            DefaultKafkaConsumerFactory<String, String> kafkaConsumerFactory =
                new DefaultKafkaConsumerFactory<>(
                    consumerConfig,
                    new StringDeserializer(),
                    new StringDeserializer());

            ContainerProperties containerProperties = new ContainerProperties(topicConfig.getTopicName());
            containerProperties.setMessageListener((MessageListener<String, String>) record -> {
                //do something with received record
                System.out.println(record);
            });

            ConcurrentMessageListenerContainer container =
                new ConcurrentMessageListenerContainer<>(
                    kafkaConsumerFactory,
                    containerProperties);

            container.start();
        });

        return null;
    }

    private Map<String, Object> buildConsumerConfig(TopicConfig topicConfig) {
        Map<String, Object> config = new HashMap<>();
        config.put(BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());

        String groupIdFromConf = topicConfig.getGroupId();
        String groupId = StringUtils.isEmpty(groupIdFromConf) ? UUID.randomUUID().toString() : groupIdFromConf;
        config.put(GROUP_ID_CONFIG, groupId);

        return Collections.unmodifiableMap(config);
    }
}
