package com.icthh.xm.commons.topic.service.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

@Getter
@ToString
@EqualsAndHashCode
public class RefreshDynamicConsumersEvent extends ApplicationEvent {

    private final String tenantKey;

    public RefreshDynamicConsumersEvent(Object source, String tenantKey) {
        super(source);
        this.tenantKey = tenantKey;
    }
}
