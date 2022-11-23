package com.icthh.xm.commons.domain.event.service.dto;

import com.icthh.xm.commons.domain.event.domain.ValidFor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent {

    @EqualsAndHashCode.Include
    private UUID id;

    private String txId;

    @Builder.Default
    private Instant eventDate = Instant.now();

    private String aggregateId;

    private String aggregateType;

    private String operation;

    private String msName;

    private String source;

    @ToString.Exclude
    private String userKey;

    private String clientId;

    private String tenant;

    @ToString.Exclude
    private ValidFor validFor;

    @ToString.Exclude
    private Map<String, Object> meta;

    @ToString.Exclude
    private DomainEventPayload payload;

}
