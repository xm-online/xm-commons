package com.icthh.xm.commons.domain.event.service.dto;

import lombok.Data;

import java.util.Map;

@Data
public class DomainEventPayload {
    private String type;
    private Map<String, Object> data;
}
