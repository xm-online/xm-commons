package com.icthh.xm.commons.domainevent.outbox.service;

import com.google.common.collect.Iterables;
import com.icthh.xm.commons.domainevent.outbox.domain.RecordStatus;
import com.icthh.xm.commons.domainevent.outbox.repository.OutboxRepository;
import com.icthh.xm.commons.domainevent.outbox.service.mapper.DomainEventMapper;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.outbox.domain.Outbox;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxTransportService {

    private final OutboxRepository outboxRepository;
    private final DomainEventMapper domainEventMapper;

    public Page<DomainEvent> findAll(Specification<Outbox> filter, Pageable pageable) {
        return outboxRepository.findAll(filter, pageable).map(domainEventMapper::toEntity);
    }

    public void changeStatus(RecordStatus status, Iterable<UUID> ids) {
        if  (ids == null || Iterables.isEmpty(ids)) {
            return;
        }
        outboxRepository.updateStatus(status, ids);
    }

    public void changeStatusById(RecordStatus status, UUID id) {
        outboxRepository.updateStatus(status, id);
    }
}
