package com.icthh.xm.commons.topic.service;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.topic.domain.DynamicConsumer;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.service.dto.RefreshDynamicConsumersEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicConsumerConfigurationService implements ApplicationListener<RefreshDynamicConsumersEvent> {

    private final List<DynamicConsumerConfiguration> dynamicConsumerConfigurations;
    private final TopicManagerService topicManagerService;
    private final TenantListRepository tenantListRepository;

    public void startDynamicConsumers(String tenantKey) {
        List<DynamicConsumer> dynamicConsumers = getDynamicConsumersByTenant(tenantKey);
        dynamicConsumers.forEach(it -> topicManagerService.startNewConsumer(tenantKey, it.getConfig(), it.getMessageHandler()));
    }

    public void refreshDynamicConsumersAll() {
        Set<String> tenants = tenantListRepository.getTenants();
        tenants.forEach(this::refreshDynamicConsumers);
    }

    public void refreshDynamicConsumers(String tenantKey) {
        List<DynamicConsumer> dynamicConsumers = getDynamicConsumersByTenant(tenantKey);

        dynamicConsumers.forEach(it -> refreshConsumer(tenantKey, it));

        List<TopicConfig> newTopicConfigs = dynamicConsumers.stream()
            .map(DynamicConsumer::getConfig)
            .collect(Collectors.toList());
        topicManagerService.removeOldConsumers(tenantKey, newTopicConfigs);
    }

    public void stopDynamicConsumers(String tenantKey) {
        topicManagerService.stopAllTenantConsumers(tenantKey);
    }

    private List<DynamicConsumer> getDynamicConsumersByTenant(String tenantKey) {
        return dynamicConsumerConfigurations.stream()
            .flatMap(it -> it.getDynamicConsumers(tenantKey).stream())
            .collect(Collectors.toList());
    }

    private void refreshConsumer(String tenantKey, DynamicConsumer updatedDynamicConsumer) {
        topicManagerService.processTopicConfig(tenantKey, updatedDynamicConsumer.getConfig(), updatedDynamicConsumer.getMessageHandler());
    }

    @Override
    public void onApplicationEvent(RefreshDynamicConsumersEvent event) {
        log.debug("OnApplicationEvent with event = {}", event);
        refreshDynamicConsumers(event.getTenantKey());
    }
}
