package com.icthh.xm.commons.domainevent.db.service;

import com.icthh.xm.commons.domainevent.db.domain.JpaEntityContext;
import com.icthh.xm.commons.domainevent.keyresolver.DomainEventFilterKeyResolver;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import org.springframework.stereotype.Component;

@Component
@LepService(group = "domainevent.db.filter")
public class DatabaseFilter {

    @LoggingAspectConfig(inputExcludeParams = "context")
    @LogicExtensionPoint(value = "DatabaseFilter", resolver = DomainEventFilterKeyResolver.class)
    public Boolean lepFiltering(String key, String tableName, JpaEntityContext context) {
        return null;
    }
}
