package com.icthh.xm.commons.topic.service;

import com.icthh.xm.commons.topic.domain.ConsumerHolder;
import com.icthh.xm.commons.topic.domain.DynamicConsumer;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DynamicConsumerConfigurationService {

    private final Map<String, Map<String, ConsumerHolder>> dynamicConsumersByTenant = new ConcurrentHashMap<>();
    private final List<DynamicConsumerConfiguration> dynamicConsumerConfigurations;
    private final TopicManagerService topicManagerService;

    public void startDynamicConsumers(String tenantKey) {
        Map<String, ConsumerHolder> tenantConsumerHolders = dynamicConsumersByTenant.computeIfAbsent(tenantKey, (key) -> new ConcurrentHashMap<>());
        List<DynamicConsumer> dynamicConsumers = getDynamicConsumersByTenant(tenantKey);

        dynamicConsumers.forEach(it -> topicManagerService.startNewConsumer(tenantKey, it.getConfig(), tenantConsumerHolders, it.getMessageHandler()));
    }

    public void refreshDynamicConsumers(String tenantKey) {
        List<DynamicConsumer> dynamicConsumers = getDynamicConsumersByTenant(tenantKey);

        dynamicConsumers.forEach(it -> updateExistingConsumer(tenantKey, it));
    }

    public void stopDynamicConsumers(String tenantKey) {
        Map<String, ConsumerHolder> tenantConsumerHolders = dynamicConsumersByTenant.get(tenantKey);
        tenantConsumerHolders.forEach((k, v) -> topicManagerService.stopAllTenantConsumers(tenantKey, tenantConsumerHolders));
        dynamicConsumersByTenant.remove(tenantKey);
    }

    private List<DynamicConsumer> getDynamicConsumersByTenant(String tenantKey) {
        return dynamicConsumerConfigurations.stream()
            .flatMap(it -> it.getDynamicConsumers(tenantKey).stream())
            .collect(Collectors.toList());
    }

    private void updateExistingConsumer(String tenantKey, DynamicConsumer updatedDynamicConsumer) {
        Map<String, ConsumerHolder> tenantConsumerHolders = dynamicConsumersByTenant.get(tenantKey);
        Optional.of(tenantConsumerHolders)
            .map(it -> it.get(updatedDynamicConsumer.getConfig().getKey()))
            .ifPresent((consumerHolder) -> topicManagerService.updateConsumer(tenantKey,
                                                                              updatedDynamicConsumer.getConfig(),
                                                                              consumerHolder,
                                                                              tenantConsumerHolders,
                                                                              updatedDynamicConsumer.getMessageHandler()));
    }
}
