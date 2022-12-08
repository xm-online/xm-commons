package com.icthh.xm.commons.domainevent.service;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;

public interface Transport {
    void send(DomainEvent event);
}
