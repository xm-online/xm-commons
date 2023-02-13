package com.icthh.xm.commons.domainevent.service.impl;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
@Service
@Scope
public abstract class KafkaTransactionSynchronizationAdapterService {

    public abstract KafkaTransactionSynchronizationAdapter getKafkaTransactionSynchronizationAdapter();

    public void send(DomainEvent domainEvent, Consumer<DomainEvent> domainEventConsumer) {
        getKafkaTransactionSynchronizationAdapter().send(domainEvent, domainEventConsumer);
    }
}
