package com.icthh.xm.commons.topic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.topic.domain.DynamicConsumer;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.domain.TopicConsumersSpec;
import com.icthh.xm.commons.topic.message.MessageHandler;
import com.icthh.xm.commons.topic.service.dto.RefreshDynamicConsumersEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TopicConfigurationService implements RefreshableConfiguration, DynamicConsumerConfiguration {

    private static final String TENANT_NAME = "tenant";

    private AntPathMatcher matcher = new AntPathMatcher();
    private ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());

    private final String configPath;

    private final MessageHandler messageHandler;

    private final ApplicationEventPublisher applicationEventPublisher;

    private Map<String, List<DynamicConsumer>> tenantTopicConsumers = new ConcurrentHashMap<>();

    public TopicConfigurationService(@Value("${spring.application.name}") String appName,
                                     ApplicationEventPublisher applicationEventPublisher,
                                     MessageHandler messageHandler) {
        this.configPath = "/config/tenants/{tenant}/" + appName + "/topic-consumers.yml";
        this.applicationEventPublisher = applicationEventPublisher;
        this.messageHandler = messageHandler;
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        refreshConfig(updatedKey, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(configPath, updatedKey);
    }

    @Override
    public List<DynamicConsumer> getDynamicConsumers(String tenantKey) {
        if (tenantTopicConsumers.containsKey(tenantKey)) {
            return tenantTopicConsumers.get(tenantKey);
        }

        return List.of();
    }

    public Map<String, List<DynamicConsumer>> getTenantTopicConsumers() {
        return Collections.unmodifiableMap(tenantTopicConsumers);
    }

    private void refreshConfig(String updatedKey, String config) {
        String tenantKey = extractTenant(updatedKey);

        if (StringUtils.isEmpty(config)) {
            tenantTopicConsumers.remove(tenantKey);
        } else {
            readSpec(updatedKey, config).ifPresentOrElse(spec -> {
                List<TopicConfig> forUpdate = spec.getTopics();
                List<DynamicConsumer> dynamicConsumers = forUpdate.stream()
                        .map(this::createDynamicConsumer)
                        .collect(Collectors.toList());
                tenantTopicConsumers.put(tenantKey, dynamicConsumers);
            }, () -> log.warn("Skip processing of configuration: [{}]. Specification is null", updatedKey));
        }

        applicationEventPublisher.publishEvent(new RefreshDynamicConsumersEvent(this, tenantKey));
    }

    private String extractTenant(final String updatedKey) {
        return matcher.extractUriTemplateVariables(configPath, updatedKey).get(TENANT_NAME);
    }

    private Optional<TopicConsumersSpec> readSpec(String updatedKey, String config) {
        try {
            return Optional.of(ymlMapper.readValue(config, TopicConsumersSpec.class));
        } catch (Exception e) {
            log.error("Error read topic specification from path: {}", updatedKey, e);
        }

        return Optional.empty();
    }

    private DynamicConsumer createDynamicConsumer(TopicConfig topicConfig) {
        DynamicConsumer dynamicConsumer = new DynamicConsumer();
        dynamicConsumer.setConfig(topicConfig);
        dynamicConsumer.setMessageHandler(messageHandler);

        return dynamicConsumer;
    }
}
