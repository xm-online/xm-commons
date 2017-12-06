package com.icthh.xm.commons.migration.db;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.MultiTenantSpringLiquibase;
import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import javax.sql.DataSource;

@Slf4j
public class XmMultiTenantSpringLiquibase extends MultiTenantSpringLiquibase {

    private ResourceLoader resourceLoader;

    public XmMultiTenantSpringLiquibase() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (getDataSource() == null && getSchemas() == null) {
            super.afterPropertiesSet();
        } else {
            if (getDataSource() == null && getSchemas() != null) {
                throw new LiquibaseException("When schemas are defined you should also define a base dataSource");
            }

            if (getDataSource() != null) {
                log.info("Schema based multitenancy enabled");
                if (CollectionUtils.isEmpty(getSchemas())) {
                    log.warn("Schemas not defined, using defaultSchema only");
                    setSchemas(new ArrayList<>());
                    getSchemas().add(getDefaultSchema());
                }

                runOnAllSchemasXm();
            }
        }
    }

    private void runOnAllSchemasXm() throws LiquibaseException {

        for (String schema : getSchemas()) {
            if (schema.equals("default")) {
                schema = null;
            }

            log.info("Initializing Liquibase for schema {}", schema);
            try {
                SpringLiquibase liquibase = getXmSpringLiquibase(getDataSource());
                liquibase.setDefaultSchema(schema);
                liquibase.afterPropertiesSet();
                log.info("Liquibase run for schema {}", schema);
            } catch (Exception e) {
                log.error("Failed to initialize Liquibase for schema {}", schema, e);
            }
        }

    }

    private SpringLiquibase getXmSpringLiquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog(getChangeLog());
        liquibase.setChangeLogParameters(getParameters());
        liquibase.setContexts(getContexts());
        liquibase.setLabels(getLabels());
        liquibase.setDropFirst(isDropFirst());
        liquibase.setShouldRun(isShouldRun());
        liquibase.setRollbackFile(getRollbackFile());
        liquibase.setResourceLoader(getResourceLoader());
        liquibase.setDataSource(dataSource);
        liquibase.setDefaultSchema(getDefaultSchema());
        return liquibase;
    }

    private  ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        super.setResourceLoader(resourceLoader);
        this.resourceLoader = resourceLoader;
    }

}
