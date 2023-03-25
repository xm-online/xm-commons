package com.icthh.xm.commons.topic.service;

import com.icthh.xm.commons.topic.domain.DynamicConsumer;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DynamicConsumerConfigurationRegistry {
    private final List<DynamicConsumerConfiguration> dynamicConsumerConfigurations;

    public DynamicConsumerConfigurationRegistry(@Lazy List<DynamicConsumerConfiguration> dynamicConsumerConfigurations) {
        this.dynamicConsumerConfigurations = dynamicConsumerConfigurations;
    }

    public List<DynamicConsumer> getDynamicConsumersByTenant(String tenantKey) {
        return dynamicConsumerConfigurations.stream()
            .flatMap(it -> it.getDynamicConsumers(tenantKey).stream())
            .collect(Collectors.toList());
    }

}
