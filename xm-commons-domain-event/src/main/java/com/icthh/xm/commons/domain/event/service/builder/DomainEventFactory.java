package com.icthh.xm.commons.domain.event.service.builder;

import com.icthh.xm.commons.domain.event.domain.enums.DefaultDomainEventOperation;
import com.icthh.xm.commons.domain.event.service.dto.DomainEvent;
import com.icthh.xm.commons.domain.event.service.dto.DomainEventPayload;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class DomainEventFactory {
    private final DomainEventBuilder defaultDomainEventBuilder;
    private final DomainEventBuilder transactionalDomainEventBuilder;

    public DomainEventFactory(@Qualifier("defaultDomainEventBuilder") DomainEventBuilder defaultDomainEventBuilder,
                              @Qualifier("txDomainEventBuilder") DomainEventBuilder transactionalDomainEventBuilder) {
        this.defaultDomainEventBuilder = defaultDomainEventBuilder;
        this.transactionalDomainEventBuilder = transactionalDomainEventBuilder;
    }

    public Factory withoutTransaction() {
        return new Factory(defaultDomainEventBuilder);
    }

    public Factory withTransaction() {
        return new Factory(transactionalDomainEventBuilder);
    }

    public static class Factory {
        private final DomainEventBuilder domainEventBuilder;

        public Factory(DomainEventBuilder domainEventBuilder) {
            this.domainEventBuilder = domainEventBuilder;
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
}
