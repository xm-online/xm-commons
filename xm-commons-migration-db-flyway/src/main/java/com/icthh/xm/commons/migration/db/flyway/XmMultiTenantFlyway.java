package com.icthh.xm.commons.migration.db.flyway;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Setter
public class XmMultiTenantFlyway implements InitializingBean {

    private DataSource dataSource;
    private List<String> schemas = Collections.emptyList();
    private String[] locations = {"classpath:db/migration"};
    private boolean baselineOnMigrate = true;
    private boolean outOfOrder;
    private boolean validateOnMigrate = true;
    private boolean shouldRun = true;
    private Map<String, String> placeholders = Collections.emptyMap();

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!shouldRun) {
            log.info("Flyway migration is disabled, skipping");
            return;
        }
        if (dataSource == null) {
            throw new IllegalStateException("DataSource must not be null for multi-tenant Flyway migration");
        }
        if (schemas.isEmpty()) {
            log.warn("No schemas defined for multi-tenant Flyway migration");
            return;
        }
        for (String schema : schemas) {
            log.info("Initializing Flyway for schema {}", schema);
            try {
                Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .locations(locations)
                    .baselineOnMigrate(baselineOnMigrate)
                    .outOfOrder(outOfOrder)
                    .validateOnMigrate(validateOnMigrate)
                    .placeholders(placeholders)
                    .load();
                flyway.migrate();
                log.info("Flyway migration completed for schema {}", schema);
            } catch (Exception e) {
                log.error("Failed to run Flyway migration for schema {}", schema, e);
                throw e;
            }
        }
    }

}
