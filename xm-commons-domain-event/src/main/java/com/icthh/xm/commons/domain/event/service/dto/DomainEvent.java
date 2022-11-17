package com.icthh.xm.commons.domain.event.service.dto;

import com.icthh.xm.commons.domain.event.domain.ValidFor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class DomainEvent {

    public DomainEvent(UUID id,
                       String txId,
                       Instant eventDate,
                       String aggregateId,
                       String aggregateType,
                       String operation,
                       String msName,
                       String source,
                       String clientId,
                       String tenant) {
        this.id = id;
        this.txId = txId;
        this.eventDate = eventDate;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.operation = operation;
        this.msName = msName;
        this.source = source;
        this.clientId = clientId;
        this.tenant = tenant;
    }

    private UUID id;
    private String txId;
    private Instant eventDate;
    private String aggregateId;
    private String aggregateType;
    private String operation;
    private String msName;
    private String source;
    private String userKey;
    private String clientId;
    private String tenant;
    private ValidFor validFor;
    private Map<String, Object> meta;
    private DomainEventPayload payload;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainEvent that = (DomainEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DomainEvent{" +
            "id=" + id +
            ", txId='" + txId + '\'' +
            ", eventDate=" + eventDate +
            ", aggregateId='" + aggregateId + '\'' +
            ", aggregateType='" + aggregateType + '\'' +
            ", operation='" + operation + '\'' +
            ", msName='" + msName + '\'' +
            ", source='" + source + '\'' +
            ", clientId='" + clientId + '\'' +
            ", tenant='" + tenant + '\'' +
            '}';
    }
}
