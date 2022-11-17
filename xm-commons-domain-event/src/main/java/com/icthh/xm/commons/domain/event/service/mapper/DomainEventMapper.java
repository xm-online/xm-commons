package com.icthh.xm.commons.domain.event.service.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.domain.event.domain.Outbox;
import com.icthh.xm.commons.domain.event.service.dto.DomainEvent;
import com.icthh.xm.commons.domain.event.service.dto.DomainEventPayload;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

@Mapper(componentModel = "spring")
public abstract class DomainEventMapper {

    @Value("${spring.application.name}")
    private String msName;
    @Autowired
    private TenantContextHolder tenantContextHolder;
    @Autowired
    private ObjectMapper objectMapper;

    @Mapping(target = "payload", source = "payload", qualifiedByName = "mapToDtoPayload")
    @Mapping(target = "status", constant = "NEW")
    public abstract Outbox toDto(DomainEvent domainEvent);

    @Mapping(target = "msName", constant = "ignored", qualifiedByName = "mapMsName")
    @Mapping(target = "tenant", constant = "ignored", qualifiedByName = "mapTenant")
    @Mapping(target = "payload", source = "payload", qualifiedByName = "mapToPayload")
    public abstract DomainEvent toEntity(Outbox outbox);

    @Named("mapMsName")
    String mapMsName(String ignored) {
        return msName;
    }

    @Named("mapTenant")
    String mapTenant(String ignored) {
        return tenantContextHolder == null ? null : tenantContextHolder.getTenantKey();
    }

    @Named("mapToPayload")
    DomainEventPayload mapToPayload(Map<String, Object> payload) throws ClassNotFoundException {
        if (payload == null) {
            return new DomainEventPayload();
        }
        Object type = payload.get("type");
        if (!(type instanceof String)) {
            type = DomainEventPayload.class.getSimpleName();
        }
        String payloadClassName = DomainEventPayload.class.getPackageName() + "." + type;

        Class payloadClass = Class.forName(payloadClassName);
        return (DomainEventPayload) objectMapper.convertValue(payload, payloadClass);
    }

    @Named("mapToDtoPayload")
    Map<String, Object> mapToDtoPayload(DomainEventPayload payload) {
        if (payload == null) {
            return new HashMap<>();
        }
        Map<String, Object> payloadMap = objectMapper.convertValue(payload, Map.class);
        payloadMap.put("type", payload.getClass().getSimpleName());
        return payloadMap;
    }
}
