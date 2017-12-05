package com.icthh.xm.commons.migration.db;

import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XmSpringLiquibase extends SpringLiquibase {

    public XmSpringLiquibase() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() {
        try {
            super.afterPropertiesSet();
        } catch (Exception e) {
            log.error("Failed to initialize default schema {}", getDefaultSchema(), e);
        }
    }

}
