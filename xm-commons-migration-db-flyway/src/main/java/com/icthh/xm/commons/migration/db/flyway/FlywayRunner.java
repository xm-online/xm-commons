package com.icthh.xm.commons.migration.db.flyway;

import com.icthh.xm.commons.tenant.TenantContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlywayRunner {

    private final DataSource dataSource;
    private final FlywayProperties flywayProperties;

    public void runOnTenant(String tenantKey, String... locations) {
        StopWatch stopWatch = StopWatch.createStarted();
        log.info("start Flyway migration for tenant {}, locations: {}", tenantKey, locations);
        try {
            String schema = TenantContextUtils.normalizeTenant(tenantKey);
            String[] migrationLocations = (locations != null && locations.length > 0)
                ? locations
                : flywayProperties.getLocations().toArray(new String[0]);

            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations(migrationLocations)
                .baselineOnMigrate(flywayProperties.isBaselineOnMigrate())
                .outOfOrder(flywayProperties.isOutOfOrder())
                .validateOnMigrate(flywayProperties.isValidateOnMigrate())
                .placeholders(flywayProperties.getPlaceholders())
                .load();
            flyway.migrate();
        } finally {
            log.info("stop  Flyway migration for tenant {}, time: {} ms", tenantKey, stopWatch.getTime());
        }
    }
}