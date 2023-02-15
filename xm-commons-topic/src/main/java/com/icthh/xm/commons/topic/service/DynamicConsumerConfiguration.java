package com.icthh.xm.commons.topic.service;

import com.icthh.xm.commons.topic.domain.DynamicConsumer;

import java.util.List;

public interface DynamicConsumerConfiguration {
    List<DynamicConsumer> getDynamicConsumers(String tenantKey);
}
