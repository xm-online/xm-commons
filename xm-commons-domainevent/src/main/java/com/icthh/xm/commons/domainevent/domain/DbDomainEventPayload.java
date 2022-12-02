package com.icthh.xm.commons.domainevent.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class DbDomainEventPayload extends DomainEventPayload {
    private Map<String, Object> before = new HashMap<>();
    private Map<String, Object> after = new HashMap<>();
}
