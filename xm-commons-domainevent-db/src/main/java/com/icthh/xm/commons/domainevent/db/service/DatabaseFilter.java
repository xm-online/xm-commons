package com.icthh.xm.commons.domainevent.db.service;

import com.icthh.xm.commons.domainevent.db.domain.JpaEntityContext;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import org.springframework.stereotype.Component;

@Component
@LepService(group = "event.db.filter")
public class DatabaseFilter {

    @LogicExtensionPoint(value = "DatabaseFilter", resolver = FilterKeyResolver.class)
    public Boolean lepFiltering(String key, String tableName, JpaEntityContext context) {
        return null;
    }
}
