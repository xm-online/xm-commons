package com.icthh.xm.commons.migration.db.flyway.tenant.provisioner;

import com.icthh.xm.commons.migration.db.flyway.FlywayRunner;
import com.icthh.xm.commons.migration.db.tenant.DropSchemaResolver;
import com.icthh.xm.commons.migration.db.tenant.provisioner.AbstractTenantDatabaseProvisioner;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Slf4j
@Service
public class TenantDatabaseFlywayProvisioner extends AbstractTenantDatabaseProvisioner {

    private static final String SPRING_PROFILE_NO_FLYWAY = "no-flyway";

    private final FlywayProperties properties;
    private final FlywayRunner flywayRunner;
    private final Environment env;

    public TenantDatabaseFlywayProvisioner(DataSource dataSource, FlywayProperties properties,
                                           DropSchemaResolver schemaDropResolver, FlywayRunner flywayRunner,
                                           Environment env,
                                           @Value("${application.db-schema-suffix:}") String dbSchemaSuffix,
                                           @Value("${spring.jpa.database:}") String jpaVendor) {
        super(dataSource, schemaDropResolver, dbSchemaSuffix, jpaVendor);
        this.properties = properties;
        this.flywayRunner = flywayRunner;
        this.env = env;
    }

    @SneakyThrows
    @Override
    protected void migrateSchema(String schema) {
        boolean enabled = !env.acceptsProfiles(Profiles.of(SPRING_PROFILE_NO_FLYWAY))
            && properties.isEnabled();

        if (!enabled) {
            log.info("Flyway tenant migration is disabled, skipping schema: {}", schema);
            return;
        }

        String[] locations = properties.getLocations().toArray(new String[0]);
        flywayRunner.runOnTenant(schema, locations);
    }
}
