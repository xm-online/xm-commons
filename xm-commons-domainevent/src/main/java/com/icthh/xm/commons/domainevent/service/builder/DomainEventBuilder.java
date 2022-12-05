package com.icthh.xm.commons.domainevent.service.builder;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;

public interface DomainEventBuilder {
    DomainEvent.DomainEventBuilder getPrefilledBuilder();
}
