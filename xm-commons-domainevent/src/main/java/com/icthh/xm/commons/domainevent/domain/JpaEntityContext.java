package com.icthh.xm.commons.domainevent.domain;

import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode
@AllArgsConstructor
public class JpaEntityContext {
    private Serializable id;
    private Object[] currentState;
    private Object[] previousState;
    private String[] propertyNames;
    private DefaultDomainEventOperation domainEventOperation;

}

