package com.icthh.xm.commons.domainevent.outbox.domain;

import com.icthh.xm.commons.domainevent.outbox.domain.converter.MapToStringConverter;
import com.icthh.xm.commons.migration.db.jsonb.Jsonb;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "outbox")
@Getter
@Setter
public class Outbox implements Serializable, Persistable<UUID> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @NotNull
    @Column(name = "tx_id")
    private String txId;

    @NotNull
    @Column(name = "event_date")
    private Instant eventDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RecordStatus status;

    @NotNull
    @Column(name = "aggregate_id")
    private String aggregateId;

    @NotNull
    @Column(name = "aggregate_type")
    private String aggregateType;

    @NotNull
    @Column(name = "operation")
    private String operation;

    @NotNull
    @Column(name = "source")
    private String source;

    @Column(name = "user_key")
    private String userKey;

    @NotNull
    @Column(name = "client_id")
    private String clientId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride( name = "validFrom", column = @Column(name = "valid_from")),
        @AttributeOverride( name = "validTo", column = @Column(name = "valid_to")),
    })
    private ValidFor validFor;

    @Jsonb
    @Convert(converter = MapToStringConverter.class)
    @Column(name = "meta")
    private Map<String, Object> meta;

    @Jsonb
    @Convert(converter = MapToStringConverter.class)
    @Column(name = "payload")
    private Map<String, Object> payload;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Outbox outbox = (Outbox) o;
        return Objects.equals(id, outbox.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
