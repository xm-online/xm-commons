package com.icthh.xm.commons.domain.event.service.builder;

import com.icthh.xm.commons.domain.event.domain.DomainEvent;

public interface DomainEventBuilder {
    DomainEvent.DomainEventBuilder getPrefilledBuilder();
}
