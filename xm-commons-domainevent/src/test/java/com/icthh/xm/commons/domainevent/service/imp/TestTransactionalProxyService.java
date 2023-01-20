package com.icthh.xm.commons.domainevent.service.imp;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TestTransactionalProxyService {

    private final SyncKafkaTransport syncKafkaTransport;

    @Transactional
    public void sendTransactional(DomainEvent domainEvent) {
        syncKafkaTransport.send(domainEvent);
        syncKafkaTransport.send(domainEvent);
    }

    public void send(DomainEvent domainEvent) {
        syncKafkaTransport.send(domainEvent);
    }

    @Transactional
    public void sendTransactionalWithError(DomainEvent domainEvent) {
        syncKafkaTransport.send(domainEvent);
        syncKafkaTransport.send(domainEvent);
        throw new IllegalArgumentException("Some error!");
    }
}
