package com.icthh.xm.commons.topic.service;

import com.icthh.xm.commons.topic.domain.DynamicConsumer;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TopicDynamicConsumerConfiguration extends AbstractDynamicConsumerConfiguration {

    private final MessageHandler messageHandler;
    private Map<String, List<DynamicConsumer>> tenantTopicConsumers = new ConcurrentHashMap<>();

    public TopicDynamicConsumerConfiguration(ApplicationEventPublisher applicationEventPublisher, MessageHandler messageHandler) {
        super(applicationEventPublisher);
        this.messageHandler = messageHandler;
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

    protected void refreshConfig(List<TopicConfig> forUpdate, String tenantKey) {
        List<DynamicConsumer> dynamicConsumers = forUpdate.stream()
            .map(this::createDynamicConsumer)
            .collect(Collectors.toList());
        tenantTopicConsumers.put(tenantKey, dynamicConsumers);
    }

    protected void remove(String tenantKey) {
        tenantTopicConsumers.remove(tenantKey);
    }

    private DynamicConsumer createDynamicConsumer(TopicConfig topicConfig) {
        DynamicConsumer dynamicConsumer = new DynamicConsumer();
        dynamicConsumer.setConfig(topicConfig);
        dynamicConsumer.setMessageHandler(messageHandler);

        return dynamicConsumer;
    }
}
