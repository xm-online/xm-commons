package com.icthh.xm.commons.domainevent.service.db;

import com.icthh.xm.commons.domainevent.domain.JpaEntityContext;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;

public interface JpaEntityMapper {

    DomainEvent maps(Object entity, JpaEntityContext jpaEntityContext);
}
