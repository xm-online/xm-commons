package com.icthh.xm.commons.domain.event.outbox.service.impl;

import com.icthh.xm.commons.domain.event.domain.DomainEvent;
import com.icthh.xm.commons.domain.event.outbox.domain.Outbox;
import com.icthh.xm.commons.domain.event.outbox.domain.RecordStatus;
import com.icthh.xm.commons.domain.event.outbox.repository.OutboxRepository;
import com.icthh.xm.commons.domain.event.outbox.service.mapper.DomainEventMapper;
import com.icthh.xm.commons.domain.event.service.Transport;
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
