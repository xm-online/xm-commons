package com.icthh.xm.commons.domainevent.db.domain;

import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Map;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@Builder
public class JpaEntityContext {
    private Object entity;
    private Serializable id;
    private Map<String, State> propertyNameToStates;
    private DefaultDomainEventOperation domainEventOperation;

}
