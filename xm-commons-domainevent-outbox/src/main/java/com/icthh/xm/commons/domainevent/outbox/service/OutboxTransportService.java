package com.icthh.xm.commons.domainevent.outbox.service;

import org.apache.commons.collections4.IterableUtils;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.outbox.domain.Outbox;
import com.icthh.xm.commons.domainevent.outbox.domain.RecordStatus;
import com.icthh.xm.commons.domainevent.outbox.repository.OutboxRepository;
import com.icthh.xm.commons.domainevent.outbox.service.mapper.DomainEventMapper;
import com.icthh.xm.commons.lep.api.LepAdditionalContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContextField;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxTransportService implements LepAdditionalContext<OutboxTransportService> {

    private final OutboxRepository outboxRepository;
    private final DomainEventMapper domainEventMapper;

    public Page<DomainEvent> findAll(Specification<Outbox> filter, Pageable pageable) {
        return outboxRepository.findAll(filter, pageable).map(domainEventMapper::toEntity);
    }

    public void changeStatus(RecordStatus status, Iterable<UUID> ids) {
        if  (ids == null || IterableUtils.isEmpty(ids)) {
            return;
        }
        outboxRepository.updateStatus(status, ids);
    }

    public void changeStatusById(RecordStatus status, UUID id) {
        outboxRepository.updateStatus(status, id);
    }

    @Override
    @IgnoreLogginAspect
    public String additionalContextKey() {
        return OutboxTransportServiceField.FIELD_NAME;
    }

    @Override
    @IgnoreLogginAspect
    public OutboxTransportService additionalContextValue() {
        return this;
    }

    @Override
    public Class<? extends LepAdditionalContextField> fieldAccessorInterface() {
        return OutboxTransportServiceField.class;
    }

    public interface OutboxTransportServiceField extends LepAdditionalContextField {
        String FIELD_NAME = "outboxTransportService";
        default OutboxTransportService getOutboxTransportService() {
            return (OutboxTransportService)get(FIELD_NAME);
        }
    }
}
