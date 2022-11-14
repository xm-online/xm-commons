package com.icthh.xm.commons.domain.event.service;

import com.icthh.xm.commons.domain.event.domain.RecordStatus;
import com.icthh.xm.commons.domain.event.repository.OutboxRepository;
import com.icthh.xm.commons.domain.event.service.dto.DomainEvent;
import com.icthh.xm.commons.domain.event.service.mapper.DomainEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxTransportService {

    private final OutboxRepository outboxRepository;
    private final DomainEventMapper domainEventMapper;

    public Page<DomainEvent> findAll(Pageable pageable) {
        return outboxRepository.findAll(pageable).map(domainEventMapper::toDto);
    }

    public void changeStatus(RecordStatus status, Iterable<UUID> ids) {
        outboxRepository.updateStatus(status, ids);
    }

    public void changeStatusById(RecordStatus status, UUID id) {
        outboxRepository.updateStatus(status, id);
    }
}
