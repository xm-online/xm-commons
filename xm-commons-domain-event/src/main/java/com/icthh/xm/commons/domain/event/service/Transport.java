package com.icthh.xm.commons.domain.event.service;

import com.icthh.xm.commons.domain.event.domain.DomainEvent;

public interface Transport {
    void send(DomainEvent event);
}
