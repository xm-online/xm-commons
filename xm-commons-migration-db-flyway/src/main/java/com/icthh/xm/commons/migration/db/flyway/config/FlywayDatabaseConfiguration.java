package com.icthh.xm.commons.migration.db.flyway.config;

import com.icthh.xm.commons.migration.db.config.AbstractDatabaseConfiguration;
import com.icthh.xm.commons.migration.db.flyway.XmMultiTenantFlyway;
import com.icthh.xm.commons.migration.db.tenant.SchemaResolver;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import javax.sql.DataSource;

@Slf4j
public abstract class FlywayDatabaseConfiguration extends AbstractDatabaseConfiguration {

    private static final String SPRING_PROFILE_NO_FLYWAY = "no-flyway";

    public FlywayDatabaseConfiguration(Environment env,
                                       JpaProperties jpaProperties,
                                       SchemaResolver schemaResolver) {
        super(env, jpaProperties, schemaResolver);
    }

    @Bean
    public Flyway flyway(DataSource dataSource, FlywayProperties flywayProperties) {
        schemaResolver.createSchemas(dataSource);

        boolean enabled = !env.acceptsProfiles(Profiles.of(SPRING_PROFILE_NO_FLYWAY))
            && flywayProperties.isEnabled();

        if (!enabled) {
            log.info("Flyway migration is disabled");
            return Flyway.configure().dataSource(dataSource).load();
        }

        log.debug("Configuring Flyway");
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(flywayProperties.getLocations().toArray(new String[0]))
            .defaultSchema(flywayProperties.getDefaultSchema())
            .baselineOnMigrate(flywayProperties.isBaselineOnMigrate())
            .outOfOrder(flywayProperties.isOutOfOrder())
            .validateOnMigrate(flywayProperties.isValidateOnMigrate())
            .load();
        flyway.migrate();
        return flyway;
    }

    @Bean
    @DependsOn("flyway")
    public XmMultiTenantFlyway multiTenantFlyway(DataSource dataSource,
                                                  FlywayProperties flywayProperties) {
        XmMultiTenantFlyway multiTenantFlyway = new XmMultiTenantFlyway();
        multiTenantFlyway.setDataSource(dataSource);
        multiTenantFlyway.setSchemas(schemaResolver.getSchemas());

        multiTenantFlyway.setLocations(flywayProperties.getLocations().toArray(new String[0]));

        multiTenantFlyway.setBaselineOnMigrate(flywayProperties.isBaselineOnMigrate());
        multiTenantFlyway.setOutOfOrder(flywayProperties.isOutOfOrder());
        multiTenantFlyway.setValidateOnMigrate(flywayProperties.isValidateOnMigrate());

        if (env.acceptsProfiles(Profiles.of(SPRING_PROFILE_NO_FLYWAY))) {
            multiTenantFlyway.setShouldRun(false);
        } else {
            multiTenantFlyway.setShouldRun(flywayProperties.isEnabled());
        }
        return multiTenantFlyway;
    }

}
