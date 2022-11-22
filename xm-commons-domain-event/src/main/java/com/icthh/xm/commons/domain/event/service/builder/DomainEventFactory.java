package com.icthh.xm.commons.domain.event.service.builder;

import com.icthh.xm.commons.domain.event.domain.enums.DefaultDomainEventOperation;
import com.icthh.xm.commons.domain.event.service.dto.DomainEvent;
import com.icthh.xm.commons.domain.event.service.dto.DomainEventPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class DomainEventFactory {
    private final DomainEventBuilder domainEventBuilder;
    private DomainEventBuilder transactionalDomainEventBuilder;

    @Autowired
    public DomainEventFactory(@Qualifier("defaultDomainEventBuilder") DomainEventBuilder defaultDomainEventBuilder,
                              @Qualifier("txDomainEventBuilder") DomainEventBuilder transactionalDomainEventBuilder) {
        this.transactionalDomainEventBuilder = transactionalDomainEventBuilder;
        this.domainEventBuilder = defaultDomainEventBuilder;
    }

    private DomainEventFactory(DomainEventBuilder domainEventBuilder) {
        this.domainEventBuilder = domainEventBuilder;
    }

    /**
     * With this configuration factory will retrieve current transaction id from database
     * and insert it into txId field. Additional database query will be executed.
     *
     * @return factory that will set current transaction id to txId field.
     */
    public DomainEventFactory withTransaction() {
        return new DomainEventFactory(transactionalDomainEventBuilder);
    }

    public DomainEvent.DomainEventBuilder builder() {
        return domainEventBuilder.getPrefilledBuilder();
    }

    public DomainEvent build() {
        return domainEventBuilder.getPrefilledBuilder().build();
    }

    public DomainEvent build(DomainEventPayload payload) {
        return builder()
            .payload(payload)
            .build();
    }

    public DomainEvent build(DefaultDomainEventOperation operation) {
        return builder()
            .operation(operation == null ? null : operation.toString())
            .build();
    }

    public DomainEvent build(DefaultDomainEventOperation operation,
                             String aggregateId,
                             String aggregateType) {
        return builder()
            .aggregateId(aggregateId)
            .aggregateType(aggregateType)
            .operation(operation == null ? null : operation.toString())
            .build();
    }

    public DomainEvent build(DefaultDomainEventOperation operation,
                             String aggregateId,
                             String aggregateType,
                             DomainEventPayload payload) {
        return builder()
            .aggregateId(aggregateId)
            .aggregateType(aggregateType)
            .operation(operation == null ? null : operation.toString())
            .payload(payload)
            .build();
    }
}
