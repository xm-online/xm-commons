package com.icthh.xm.commons.domainevent.db.service.mapper;

import com.icthh.xm.commons.domainevent.db.domain.JpaEntityContext;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;

public interface JpaEntityMapper {

    DomainEvent map(JpaEntityContext jpaEntityContext);
}
