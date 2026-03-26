package com.icthh.xm.commons.topic.service;

import com.icthh.xm.commons.logging.trace.TraceWrapper;
import com.icthh.xm.commons.topic.config.MessageListenerContainerBuilder;
import com.icthh.xm.commons.topic.domain.ConsumerHolder;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class TopicManagerService {

    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TraceWrapper traceWrapper;
    private final Map<String, Map<String, ConsumerHolder>> topicConsumerHolders = new ConcurrentHashMap<>();

    public void processTopicConfig(String tenantKey,
                                   TopicConfig topicConfig,
                                   MessageHandler messageHandler) {
        String topicConfigKey = topicConfig.getKey();
        Map<String, ConsumerHolder> existingConsumers = getConsumerHoldersByTenant(tenantKey);
        ConsumerHolder existingConfig = existingConsumers.get(topicConfigKey);

        if (topicConfig.getMaxPollInterval() != null
            && topicConfig.getBackOffPeriod() > topicConfig.getMaxPollInterval()) {
            log.error("Consumer was not created, backOffPeriod is greater than maxPollInterval, topicConfig: [{}]",
                topicConfig);
            return;
        }

        if (existingConfig == null) {
            startNewConsumer(tenantKey, topicConfig, messageHandler);
            return;
        }

        if (existingConfig.getTopicConfig().equals(topicConfig)) {
            log.info("[{}] Skip consumer configuration due to no changes found: [{}] ", tenantKey, topicConfig);
            return;
        }

        updateConsumer(tenantKey, topicConfig, existingConfig, messageHandler);
    }

    public void stopAllTenantConsumers(String tenantKey) {
        Map<String, ConsumerHolder> existingConsumers = getConsumerHoldersByTenant(tenantKey);
        Collection<ConsumerHolder> holders = existingConsumers.values();
        withLog(tenantKey, "stopAllTenantConsumers", () -> {
            holders.forEach(consumerHolder -> stopConsumer(tenantKey, consumerHolder));
        }, "[{}]", holders);
        topicConsumerHolders.remove(tenantKey);
    }

    public void removeOldConsumers(String tenantKey,
                                   List<TopicConfig> newTopicConfigs) {
        Map<String, ConsumerHolder> existingConsumers = getConsumerHoldersByTenant(tenantKey);
        Set<Map.Entry<String, ConsumerHolder>> toRemove = existingConsumers
            .entrySet()
            .stream()
            .filter(entry -> !newTopicConfigs.contains(entry.getValue().getTopicConfig()))
            .peek(entry -> stopConsumer(tenantKey, entry.getValue()))
            .collect(Collectors.toSet());

        existingConsumers.entrySet().removeAll(toRemove);
    }

    public void startNewConsumer(String tenantKey,
                                 TopicConfig topicConfig,
                                 MessageHandler messageHandler) {
        withLog(tenantKey, "startNewConsumer", () -> {
            AbstractMessageListenerContainer container = buildListenerContainer(tenantKey, topicConfig, messageHandler);
            container.start();
            Map<String, ConsumerHolder> existingConsumers = getConsumerHoldersByTenant(tenantKey);
            existingConsumers.put(topicConfig.getKey(), new ConsumerHolder(topicConfig, container));
        }, "{}", topicConfig);
    }

    public void updateConsumer(String tenantKey,
                                TopicConfig topicConfig,
                                ConsumerHolder existingConfig,
                                MessageHandler messageHandler) {
        withLog(tenantKey, "restartConsumer", () -> {
            existingConfig.getContainer().stop();
            AbstractMessageListenerContainer container = buildListenerContainer(tenantKey, topicConfig, messageHandler);
            container.start();
            Map<String, ConsumerHolder> existingConsumers = getConsumerHoldersByTenant(tenantKey);
            existingConsumers.put(topicConfig.getKey(), new ConsumerHolder(topicConfig, container));
        }, "{}", topicConfig);
    }

    public Map<String, ConsumerHolder> getConsumerHoldersByTenantImmutable(String tenantKey) {
        return Collections.unmodifiableMap(getConsumerHoldersByTenant(tenantKey));
    }

    protected AbstractMessageListenerContainer buildListenerContainer(String tenantKey, TopicConfig topicConfig, MessageHandler messageHandler) {
        return new MessageListenerContainerBuilder(kafkaProperties, kafkaTemplate)
            .build(tenantKey, topicConfig, messageHandler, traceWrapper);
    }

    private Map<String, ConsumerHolder> getConsumerHoldersByTenant(String tenantKey) {
        return topicConsumerHolders.computeIfAbsent(tenantKey, (key) -> new ConcurrentHashMap<>());
    }

    private void withLog(String tenant, String command, Runnable action, String logTemplate, Object... params) {
        final StopWatch stopWatch = StopWatch.createStarted();
        log.info("[{}] start: {} " + logTemplate, tenant, command, params);
        action.run();
        log.info("[{}]  stop: {}, time = {} ms.", tenant, command, stopWatch.getTime());
    }

    private void stopConsumer(final String tenantKey, final ConsumerHolder consumerHolder) {
        TopicConfig existConfig = consumerHolder.getTopicConfig();
        withLog(tenantKey, "stopConsumer",
            () -> consumerHolder.getContainer().stop(), "{}", existConfig);
    }
}
