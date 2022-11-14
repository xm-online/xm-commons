package com.icthh.xm.commons.domain.event.service.impl;

import com.icthh.xm.commons.domain.event.domain.Outbox;
import com.icthh.xm.commons.domain.event.domain.RecordStatus;
import com.icthh.xm.commons.domain.event.repository.OutboxRepository;
import com.icthh.xm.commons.domain.event.service.Transport;
import com.icthh.xm.commons.domain.event.service.dto.DomainEvent;
import com.icthh.xm.commons.domain.event.service.mapper.DomainEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxTransport implements Transport {

    private final OutboxRepository outboxRepository;

    private final DomainEventMapper domainEventMapper;

    @Override
    public void send(DomainEvent event) {
        Outbox outbox = domainEventMapper.toEntity(event);
        outbox.setStatus(RecordStatus.NEW);
        outboxRepository.save(outbox);
    }
}
