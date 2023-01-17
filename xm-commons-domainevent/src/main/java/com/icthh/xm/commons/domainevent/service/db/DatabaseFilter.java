package com.icthh.xm.commons.domainevent.service.db;

import com.icthh.xm.commons.domainevent.domain.JpaEntityContext;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import org.springframework.stereotype.Component;

@Component
public class DatabaseFilter {

    @LogicExtensionPoint(value = "DatabaseFilter", resolver = FilterKeyResolver.class)
    public Boolean lepFiltering(String key, String tableName, JpaEntityContext context) {
        return null;
    }
}
