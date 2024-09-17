package com.icthh.xm.commons.topic.service;

import com.icthh.xm.commons.topic.service.dto.RefreshDynamicConsumersEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@RequiredArgsConstructor
public abstract class AbstractDynamicConsumerConfiguration implements DynamicConsumerConfiguration {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void sendRefreshDynamicConsumersEvent(String tenantKey) {
        applicationEventPublisher.publishEvent(new RefreshDynamicConsumersEvent(this, tenantKey));
    }
}
