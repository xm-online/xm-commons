package com.icthh.xm.commons.domainevent.outbox.service.impl;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.outbox.domain.Outbox;
import com.icthh.xm.commons.domainevent.outbox.domain.RecordStatus;
import com.icthh.xm.commons.domainevent.outbox.repository.OutboxRepository;
import com.icthh.xm.commons.domainevent.outbox.service.mapper.DomainEventMapper;
import com.icthh.xm.commons.domainevent.service.Transport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxTransport implements Transport {

    private final OutboxRepository outboxRepository;

    private final DomainEventMapper domainEventMapper;

    @Override
    public void send(DomainEvent event) {
        Outbox outbox = domainEventMapper.toDto(event);
        outbox.setStatus(RecordStatus.NEW);
        outboxRepository.save(outbox);
    }
}
