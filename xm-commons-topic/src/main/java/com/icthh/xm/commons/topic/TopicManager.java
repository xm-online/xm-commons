package com.icthh.xm.commons.topic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.topic.message.MessageHandler;
import com.icthh.xm.commons.topic.spec.TopicConsumersSpec;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicManager implements RefreshableConfiguration {

    private static final String CONSUMER_CONFIG_PATH_PATTERN = "/config/tenants/{tenant}/{ms}/topic-consumers.yml";
    private static final String TENANT_NAME = "tenant";

    private AntPathMatcher matcher = new AntPathMatcher();
    private ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());


    private final KafkaProperties kafkaProperties;
    private final MessageHandler messageHandler;

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
        //todo manage config, on refresh restart all?
        //todo unique key for configuration
        String tenantKey = extractTenant(updatedKey);

        TopicConsumersSpec spec = readSpec(updatedKey, config);
        topicConsumers.put(tenantKey, spec);

        initConsumers(tenantKey, spec);
        //log.info("Tasks for tenant '{}' were updated: {}", tenant, updatedKey);

    }

    private String extractTenant(final String updatedKey) {
        return matcher.extractUriTemplateVariables(CONSUMER_CONFIG_PATH_PATTERN, updatedKey).get(TENANT_NAME);
    }

    private List<Object> initConsumers(String tenantKey, TopicConsumersSpec spec) {
        spec.getTopics().forEach(topicConfig -> {

            AbstractMessageListenerContainer container =
                new MessageListenerContainerBuilder(kafkaProperties, messageHandler)
                    .build(tenantKey, topicConfig);

            if (!StringUtils.isEmpty(topicConfig.getDeadLetterQueue())) {
                //todo build producer
            }


            container.start();
        });

        return null;
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


}
