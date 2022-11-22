package com.icthh.xm.commons.migration.db.liquibase;

import com.icthh.xm.commons.tenant.TenantContextUtils;
import liquibase.integration.spring.SpringLiquibase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiquibaseRunner {

    private final ResourceLoader resourceLoader;
    private final DataSource dataSource;
    private final LiquibaseProperties liquibaseProperties;

    public void runOnTenant(String tenantKey, String changelogPath) {
        StopWatch stopWatch = StopWatch.createStarted();
        log.info("start Liquibase migration for tenant {}, changelog path: {}", tenantKey, changelogPath);
        try {
            String schema = TenantContextUtils.normalizeTenant(tenantKey);
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setResourceLoader(resourceLoader);
            liquibase.setDataSource(dataSource);
            liquibase.setChangeLog(changelogPath);
            liquibase.setContexts(liquibaseProperties.getContexts());
            liquibase.setDefaultSchema(schema);
            liquibase.setDropFirst(liquibaseProperties.isDropFirst());
            liquibase.setChangeLogParameters(liquibaseProperties.getParameters());
            liquibase.setShouldRun(liquibaseProperties.isEnabled());
        } finally {
            log.info("stop  Liquibase migration for tenant {}, time: {} ms", tenantKey, stopWatch.getTime());
        }
    }
}
