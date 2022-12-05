package com.icthh.xm.commons.domainevent.service.builder;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.domain.DomainEventPayload;
import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventOperation;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DomainEventFactory {
    private final DomainEventBuilder domainEventBuilder;
    private Optional<DomainEventBuilder> transactionalDomainEventBuilder;

    @Autowired
    public DomainEventFactory(@Qualifier("defaultDomainEventBuilder") DomainEventBuilder defaultDomainEventBuilder,
                              @Qualifier("txDomainEventBuilder")
                              Optional<DomainEventBuilder> transactionalDomainEventBuilder) {
        this.transactionalDomainEventBuilder = transactionalDomainEventBuilder;
        this.domainEventBuilder = defaultDomainEventBuilder;
    }

    private DomainEventFactory(DomainEventBuilder domainEventBuilder) {
        this.domainEventBuilder = domainEventBuilder;
    }

    /**
     * With this configuration factory will retrieve current transaction id from database
     * and insert it into txId field. Additional database query will be executed.
     * If method is called multiple times for the same transaction, query will be run only once,
     * next method calls will return cached transaction id.
     *
     * @return factory that will set current transaction id to txId field.
     */
    public DomainEventFactory withTransaction() {
        DomainEventBuilder txDomainEventBuilder = transactionalDomainEventBuilder.orElseThrow(() ->
                        new NotImplementedException("Transactional domain event builder is not implemented."
                                + " No txDomainEventBuilder bean in scope.")
                );
        return new DomainEventFactory(txDomainEventBuilder);
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
