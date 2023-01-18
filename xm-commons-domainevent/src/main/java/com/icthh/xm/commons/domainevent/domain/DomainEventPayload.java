package com.icthh.xm.commons.domainevent.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = DomainEventPayload.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = DbDomainEventPayload.class, name = "DbDomainEventPayload"),
    @JsonSubTypes.Type(value = HttpDomainEventPayload.class, name = "HttpDomainEventPayload")
})
public class DomainEventPayload {
    private Map<String, Object> data;

    public DomainEventPayload(Map<String, Object> data) {
        this.data = data;
    }
}
