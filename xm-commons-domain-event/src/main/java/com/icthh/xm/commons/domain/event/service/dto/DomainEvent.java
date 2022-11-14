package com.icthh.xm.commons.domain.event.service.dto;

import com.icthh.xm.commons.domain.event.domain.ValidFor;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
public class DomainEvent {
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
}
