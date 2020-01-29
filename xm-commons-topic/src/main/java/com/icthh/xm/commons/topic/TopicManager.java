package com.icthh.xm.commons.topic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.topic.config.MessageListenerContainerBuilder;
import com.icthh.xm.commons.topic.domain.ConsumerHolder;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.domain.TopicConsumersSpec;

import com.icthh.xm.commons.topic.message.MessageHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TopicManager implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenant";

    private AntPathMatcher matcher = new AntPathMatcher();
    private ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());

    private final String configPath;

    @Getter
    private Map<String, Map<String, ConsumerHolder>> tenantTopicConsumers = new ConcurrentHashMap<>();

    public TopicManager(@Value("${spring.application.name}") String appName,
                        KafkaProperties kafkaProperties,
                        KafkaTemplate<String, String> kafkaTemplate,
                        MessageHandler messageHandler) {
        this.kafkaProperties = kafkaProperties;
        this.kafkaTemplate = kafkaTemplate;
        this.messageHandler = messageHandler;
        this.configPath = "/config/tenants/{tenant}/" + appName + "/topic-consumers.yml";
    }

    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MessageHandler messageHandler;

    @Override
    public void onRefresh(String updatedKey, String config) {
        refreshConfig(updatedKey, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(configPath, updatedKey);
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
            stopAllTenantConsumers(tenantKey, existingConsumers);
            return;
        }
        TopicConsumersSpec spec = readSpec(updatedKey, config);
        if (spec == null) {
            log.warn("Skip processing of configuration: [{}]. Specification is null", updatedKey);
            return;
        }
        List<TopicConfig> forUpdate = spec.getTopics();

        //start and update consumers
        forUpdate.forEach(topicConfig -> processTopicConfig(tenantKey, topicConfig, existingConsumers));

        //remove old consumers
        removeOldConsumers(tenantKey, forUpdate, existingConsumers);

        tenantTopicConsumers.put(tenantKey, existingConsumers);
    }

    private void processTopicConfig(String tenantKey,
                                    TopicConfig topicConfig,
                                    Map<String, ConsumerHolder> existingConsumers) {
        String topicConfigKey = topicConfig.getKey();
        ConsumerHolder existingConfig = existingConsumers.get(topicConfigKey);

        if (topicConfig.getMaxPollInterval() != null
              && topicConfig.getBackOffPeriod() > topicConfig.getMaxPollInterval()) {
            log.error("Consumer was not created, backOffPeriod is greater than maxPollInterval, topicConfig: [{}]",
                      topicConfig);
            return;
        }

        if (existingConfig == null) {
            startNewConsumer(tenantKey, topicConfig, existingConsumers);
            return;
        }

        if (existingConfig.getTopicConfig().equals(topicConfig)) {
            log.info("[{}] Skip consumer configuration due to no changes found: [{}] ", tenantKey, topicConfig);
            return;
        }

        updateConsumer(tenantKey, topicConfig, existingConfig, existingConsumers);
    }

    private void startNewConsumer(String tenantKey,
                                  TopicConfig topicConfig,
                                  Map<String, ConsumerHolder> existingConsumers) {
        withLog(tenantKey, "startNewConsumer", () -> {
            AbstractMessageListenerContainer container = buildListenerContainer(tenantKey, topicConfig);
            container.start();
            existingConsumers.put(topicConfig.getKey(), new ConsumerHolder(topicConfig, container));
        }, "{}", topicConfig);
    }

    private void updateConsumer(String tenantKey,
                                TopicConfig topicConfig,
                                ConsumerHolder existingConfig,
                                Map<String, ConsumerHolder> existingConsumers) {
        withLog(tenantKey, "restartConsumer", () -> {
            existingConfig.getContainer().stop();
            AbstractMessageListenerContainer container = buildListenerContainer(tenantKey, topicConfig);
            container.start();
            existingConsumers.put(topicConfig.getKey(), new ConsumerHolder(topicConfig, container));
        }, "{}", topicConfig);
    }

    protected AbstractMessageListenerContainer buildListenerContainer(String tenantKey, TopicConfig topicConfig) {
        return new MessageListenerContainerBuilder(kafkaProperties, kafkaTemplate)
            .build(tenantKey, topicConfig, messageHandler);
    }

    private void stopAllTenantConsumers(String tenantKey,
                                        Map<String, ConsumerHolder> existingConsumers) {
        Collection<ConsumerHolder> holders = existingConsumers.values();
        withLog(tenantKey, "stopAllTenantConsumers", () -> {
            holders.forEach(consumerHolder -> stopConsumer(tenantKey, consumerHolder));
            tenantTopicConsumers.remove(tenantKey);
        }, "[{}]", holders);
    }

    private void removeOldConsumers(String tenantKey,
                                    List<TopicConfig> newTopicConfigs,
                                    Map<String, ConsumerHolder> existingConsumers) {

        Set<Map.Entry<String, ConsumerHolder>> toRemove = existingConsumers
            .entrySet()
            .stream()
            .filter(entry -> !newTopicConfigs.contains(entry.getValue().getTopicConfig()))
            .peek(entry -> stopConsumer(tenantKey, entry.getValue()))
            .collect(Collectors.toSet());

        existingConsumers.entrySet().removeAll(toRemove);
    }

    private void stopConsumer(final String tenantKey, final ConsumerHolder consumerHolder) {
        TopicConfig existConfig = consumerHolder.getTopicConfig();
        withLog(tenantKey, "stopConsumer",
            () -> consumerHolder.getContainer().stop(), "{}", existConfig);
    }

    private String extractTenant(final String updatedKey) {
        return matcher.extractUriTemplateVariables(configPath, updatedKey).get(TENANT_NAME);
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
        if (tenantTopicConsumers.containsKey(tenantKey)) {
            return tenantTopicConsumers.get(tenantKey);
        } else {
            return new ConcurrentHashMap<>();
        }
    }

    private void withLog(String tenant, String command, Runnable action, String logTemplate, Object... params) {
        final StopWatch stopWatch = StopWatch.createStarted();
        log.info("[{}] start: {} " + logTemplate, tenant, command, params);
        action.run();
        log.info("[{}]  stop: {}, time = {} ms.", tenant, command, stopWatch.getTime());
    }
}
