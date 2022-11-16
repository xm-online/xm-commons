package com.icthh.xm.commons.domain.event.config;

import com.icthh.xm.commons.domain.event.util.TenantUtil;
import liquibase.integration.spring.SpringLiquibase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({LiquibaseProperties.class})
public class LiquibaseRunner {

    private static final String CHANGE_LOG_PATH
        = "classpath:config/liquibase/changelog/outbox/db.changelog-master.yaml";

    private final ResourceLoader resourceLoader;
    private final DataSource dataSource;
    private final LiquibaseProperties liquibaseProperties;

    public void runOnTenant(String tenant) {
        String schema = TenantUtil.normalizeTenant(tenant);
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setResourceLoader(resourceLoader);
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(CHANGE_LOG_PATH);
        liquibase.setContexts(liquibaseProperties.getContexts());
        liquibase.setDefaultSchema(schema);
        liquibase.setDropFirst(liquibaseProperties.isDropFirst());
        liquibase.setChangeLogParameters(liquibaseProperties.getParameters());
        liquibase.setShouldRun(liquibaseProperties.isEnabled());
        try {
            liquibase.afterPropertiesSet();
            log.info("Domain event liquibase is running for schema {}", schema);
        } catch (Exception e) {
            log.error("Failed to initialize Liquibase for schema {}", schema, e);
        }
    }
}
