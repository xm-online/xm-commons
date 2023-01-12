package com.icthh.xm.commons.domainevent.domain;

import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.type.Type;

import java.io.Serializable;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@Builder
public class JpaEntityContext {
    private Object entity;
    private Serializable id;
    private Object[] currentState;
    private Object[] previousState;
    private String[] propertyNames;
    private Type[] types;
    private DefaultDomainEventOperation domainEventOperation;

}

