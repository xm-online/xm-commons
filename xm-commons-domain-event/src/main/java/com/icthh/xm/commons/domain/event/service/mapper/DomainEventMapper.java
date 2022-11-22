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

import java.util.Map;

@Mapper(componentModel = "spring")
public abstract class DomainEventMapper {

    private String msName;
    private TenantContextHolder tenantContextHolder;
    private ObjectMapper objectMapper;

    @Value("${spring.application.name}")
    public void setMsName(String msName) {
        this.msName = msName;
    }

    @Autowired
    public void setTenantContextHolder(TenantContextHolder tenantContextHolder) {
        this.tenantContextHolder = tenantContextHolder;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();
    }

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
        return objectMapper.convertValue(payload, DomainEventPayload.class);
    }

    @Named("mapToDtoPayload")
    Map<String, Object> mapToDtoPayload(DomainEventPayload payload) {
        return objectMapper.convertValue(payload, Map.class);
    }
}
